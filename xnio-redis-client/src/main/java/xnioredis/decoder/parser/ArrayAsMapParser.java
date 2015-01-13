package xnioredis.decoder.parser;

import xnioredis.decoder.MapBuilderFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class ArrayAsMapParser<T, K, V> implements Parser<T> {
    private final int len;
    private final MapBuilderFactory<K, V, ? extends T> builderFactory;
    private final SeqParser<K, V> kvParser;

    public ArrayAsMapParser(int len, MapBuilderFactory<K, V, ? extends T> builderFactory, SeqParser<K, V> kvParser) {
        this.len = len;
        this.builderFactory = builderFactory;
        this.kvParser = kvParser;
    }

    @Override
    public <U> U parse(ByteBuffer buffer, Visitor<? super T, U> visitor) {
        return doParse(buffer, visitor, builderFactory.create(len), len, kvParser);
    }

    private <U> U doParse(ByteBuffer buffer, Visitor<? super T, U> visitor, MapBuilderFactory.Builder<K, V, ? extends T> builder, int remaining, SeqParser<? extends K, ? extends V> kvSeqParser) {
        if (remaining == 0) {
            return visitor.success(builder.build());
        } else {
            return kvSeqParser.parse(buffer, new SeqParser.Visitor<K, V, U>() {
                @Override
                public U success(@Nullable K value1, @Nullable V value2) {
                    builder.put(value1, value2);
                    return doParse(buffer, visitor, builder, remaining - 1, kvParser);
                }

                @Override
                public U partial(SeqParser<? extends K, ? extends V> partial) {
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
