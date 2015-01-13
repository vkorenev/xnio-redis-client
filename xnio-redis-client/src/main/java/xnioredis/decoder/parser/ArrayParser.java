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
    public <U> U parse(ByteBuffer buffer, Visitor<? super T, U> visitor) {
        return doParse(buffer, visitor, builderFactory.create(len), len, elementParser);
    }

    private <U> U doParse(ByteBuffer buffer, Visitor<? super T, U> visitor, ArrayBuilderFactory.Builder<E, ? extends T> builder, int remaining, Parser<? extends E> elemParser) {
        if (remaining == 0) {
            return visitor.success(builder.build());
        } else {
            return elemParser.parse(buffer, new Visitor<E, U>() {
                @Override
                public U success(@Nullable E value) {
                    builder.add(value);
                    return doParse(buffer, visitor, builder, remaining - 1, elementParser);
                }

                @Override
                public U partial(Parser<? extends E> partial) {
                    return visitor.partial(new Parser<T>() {
                        @Override
                        public <U1> U1 parse(ByteBuffer buffer, Visitor<? super T, U1> visitor) {
                            return doParse(buffer, visitor, builder, remaining, partial);
                        }
                    });
                }
            });
        }
    }
}
