package xnioredis.decoder.parser;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Function;

public interface Parser<T> extends ReplyParser<T> {
    @Override
    Result<T> parse(ByteBuffer buffer);

    @Override
    default <R> Parser<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return buffer -> {
            Result<T> result = Parser.this.parse(buffer);
            return new Result<R>() {
                @Override
                public <U> U accept(Visitor<? super R, U> visitor) {
                    return result.accept(new Visitor<T, U>() {
                        @Override
                        public U success(@Nullable T value) {
                            return visitor.success(mapper.apply(value));
                        }

                        @Override
                        public U partial(Parser<? extends T> partial) {
                            return visitor.partial(partial.map(mapper));
                        }
                    });
                }
            };
        };
    }

    interface Result<T> extends ReplyParser.Result<T> {
        <U> U accept(Visitor<? super T, U> visitor);

        @Override
        default <U> U acceptReply(ReplyVisitor<? super T, U> visitor) {
            return accept(visitor);
        }
    }

    interface Visitor<T, U> {
        U success(@Nullable T value);

        U partial(Parser<? extends T> partial);
    }

    class Success<T> implements Result<T> {
        private final T value;

        public Success(@Nullable T value) {
            this.value = value;
        }

        @Override
        public <U> U accept(Visitor<? super T, U> visitor) {
            return visitor.success(value);
        }
    }

    interface Partial<T> extends Result<T>, Parser<T> {
        @Override
        default <U> U accept(Visitor<? super T, U> visitor) {
            return visitor.partial(this);
        }
    }
}
