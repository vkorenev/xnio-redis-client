package xnioredis.decoder.parser;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Function;

public interface ReplyParser<T> {
    Result<? extends T> parse(ByteBuffer buffer);

    default <R> ReplyParser<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return buffer -> {
            Result<? extends T> result = ReplyParser.this.parse(buffer);
            return new Result<R>() {
                @Override
                public <U> U acceptReply(ReplyVisitor<? super R, U> visitor) {
                    return result.acceptReply(new ReplyVisitor<T, U>() {
                        @Override
                        public U success(@Nullable T value) {
                            return visitor.success(mapper.apply(value));
                        }

                        @Override
                        public U failure(CharSequence message) {
                            return visitor.failure(message);
                        }

                        @Override
                        public U partialReply(ReplyParser<? extends T> partial) {
                            return visitor.partialReply(partial.map(mapper));
                        }
                    });
                }
            };
        };
    }

    interface Result<T> {
        <U> U acceptReply(ReplyVisitor<? super T, U> visitor);
    }

    interface ReplyVisitor<T, U> extends Parser.Visitor<T, U> {
        U failure(CharSequence message);

        U partialReply(ReplyParser<? extends T> partial);

        @Override
        default U partial(Parser<? extends T> partial) {
            return partialReply(partial);
        }
    }

    class Failure<T> implements Result<T> {
        public final CharSequence message;

        public Failure(CharSequence message) {
            this.message = message;
        }

        @Override
        public <U> U acceptReply(ReplyVisitor<? super T, U> visitor) {
            return visitor.failure(message);
        }
    }

    interface Partial<T> extends Result<T>, ReplyParser<T> {
        @Override
        default <U> U acceptReply(ReplyVisitor<? super T, U> visitor) {
            return visitor.partialReply(this);
        }
    }
}
