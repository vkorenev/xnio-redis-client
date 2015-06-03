package xnioredis.decoder.parser;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Objects;
import java.util.function.Function;

public interface Parser<T> extends ReplyParser<T> {
    <U> U parse(ByteBuffer buffer, Visitor<? super T, U> visitor, CharsetDecoder charsetDecoder);

    @Override
    default <U> U parseReply(ByteBuffer buffer, ReplyVisitor<? super T, U> visitor, CharsetDecoder charsetDecoder) {
        return parse(buffer, visitor, charsetDecoder);
    }

    @Override
    default <R> Parser<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return new Parser<R>() {
            @Override
            public <U> U parse(ByteBuffer buffer, Visitor<? super R, U> visitor, CharsetDecoder charsetDecoder) {
                return Parser.this.parse(buffer, new Visitor<T, U>() {
                    @Override
                    public U success(@Nullable T value) {
                        return visitor.success(mapper.apply(value));
                    }

                    @Override
                    public U partial(Parser<? extends T> partial) {
                        return visitor.partial(partial.map(mapper));
                    }
                }, charsetDecoder);
            }
        };
    }

    interface Visitor<T, U> {
        U success(@Nullable T value);

        U partial(Parser<? extends T> partial);
    }
}
