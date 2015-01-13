package xnioredis.decoder.parser;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Function;

public interface ReplyParser<T> {
    <U> U parseReply(ByteBuffer buffer, ReplyVisitor<? super T, U> visitor);

    default <R> ReplyParser<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return new ReplyParser<R>() {
            @Override
            public <U> U parseReply(ByteBuffer buffer, ReplyVisitor<? super R, U> visitor) {
                return ReplyParser.this.parseReply(buffer, new ReplyVisitor<T, U>() {
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
    }

    interface ReplyVisitor<T, U> extends Parser.Visitor<T, U> {
        U failure(CharSequence message);

        U partialReply(ReplyParser<? extends T> partial);

        @Override
        default U partial(Parser<? extends T> partial) {
            return partialReply(partial);
        }
    }
}
