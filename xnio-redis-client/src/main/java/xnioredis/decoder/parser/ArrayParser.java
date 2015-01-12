package xnioredis.decoder.parser;

import xnioredis.decoder.ArrayBuilderFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class ArrayParser<T, E> implements Parser<T> {
    private final int len;
    private final ArrayBuilderFactory<E, ? extends T> builderFactory;
    private final Parser<E> elementParser;

    public ArrayParser(int len, ArrayBuilderFactory<E, ? extends T> builderFactory, Parser<E> elementParser) {
        this.len = len;
        this.builderFactory = builderFactory;
        this.elementParser = elementParser;
    }

    @Override
    public Result<T> parse(ByteBuffer buffer) {
        ArrayBuilderFactory.Builder<E, ? extends T> builder = builderFactory.create(len);
        if (len == 0) {
            return new Success<>(builder.build());
        } else {
            return doParse(buffer, builder, len, elementParser);
        }
    }

    private Result<T> doParse(ByteBuffer buffer, ArrayBuilderFactory.Builder<E, ? extends T> builder, int remaining, Parser<? extends E> elementParser) {
        Visitor<E, Result<T>> visitor = new Visitor<E, Result<T>>() {
            private int r = remaining;

            @Override
            public Result<T> success(@Nullable E value) {
                builder.add(value);
                r--;
                return r == 0 ? new Success<>(builder.build()) : null;
            }

            @Override
            public Result<T> partial(Parser<? extends E> partial) {
                return (Partial<T>) buffer -> doParse(buffer, builder, r, partial);
            }
        };
        Result<T> result;
        while ((result = elementParser.parse(buffer).accept(visitor)) == null) {
            elementParser = this.elementParser;
        }
        return result;
    }
}
