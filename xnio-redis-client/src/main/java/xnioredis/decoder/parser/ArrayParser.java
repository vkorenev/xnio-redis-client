package xnioredis.decoder.parser;

import xnioredis.decoder.ArrayBuilderFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

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
    public <U> U parse(ByteBuffer buffer, Visitor<? super T, U> visitor, CharsetDecoder charsetDecoder) {
        return doParse(buffer, visitor, builderFactory.create(len), len, elementParser, charsetDecoder);
    }

    private <U> U doParse(ByteBuffer buffer, Visitor<? super T, U> visitor,
            ArrayBuilderFactory.Builder<E, ? extends T> builder, int remaining,
            Parser<? extends E> elemParser, CharsetDecoder charsetDecoder) {
        while (remaining > 0) {
            Parser<T> partial = parsePartial(buffer, builder, remaining, elemParser, charsetDecoder);
            if (partial != null) {
                return visitor.partial(partial);
            } else {
                remaining--;
                elemParser = elementParser;
            }
        }
        return visitor.success(builder.build());
    }

    private Parser<T> parsePartial(ByteBuffer buffer, ArrayBuilderFactory.Builder<E, ? extends T> builder,
            int remaining, Parser<? extends E> elemParser, CharsetDecoder charsetDecoder) {
        return elemParser.parse(buffer, new Visitor<E, Parser<T>>() {
            @Override
            public Parser<T> success(@Nullable E value) {
                builder.add(value);
                return null;
            }

            @Override
            public Parser<T> partial(Parser<? extends E> partial) {
                return new Parser<T>() {
                    @Override
                    public <U1> U1 parse(ByteBuffer buffer, Visitor<? super T, U1> visitor,
                            CharsetDecoder charsetDecoder) {
                        return doParse(buffer, visitor, builder, remaining, partial, charsetDecoder);
                    }
                };
            }
        }, charsetDecoder);
    }
}
