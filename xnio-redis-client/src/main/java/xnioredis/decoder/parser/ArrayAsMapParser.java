package xnioredis.decoder.parser;

import xnioredis.decoder.MapBuilderFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class ArrayAsMapParser<T, K, V> implements Parser<T> {
    private final int len;
    private final MapBuilderFactory<K, V, ? extends T> builderFactory;
    private final SeqParser<K, V> elementParser;

    public ArrayAsMapParser(int len, MapBuilderFactory<K, V, ? extends T> builderFactory, SeqParser<K, V> elementParser) {
        this.len = len;
        this.builderFactory = builderFactory;
        this.elementParser = elementParser;
    }

    @Override
    public Result<T> parse(ByteBuffer buffer) {
        MapBuilderFactory.Builder<K, V, ? extends T> builder = builderFactory.create(len);
        if (len == 0) {
            return new Success<>(builder.build());
        } else {
            return doParse(buffer, builder, len, elementParser);
        }
    }

    private Result<T> doParse(ByteBuffer buffer, MapBuilderFactory.Builder<K, V, ? extends T> builder, int remaining, SeqParser<K, V> kvSeqParser) {
        SeqParser.Visitor<K, V, Result<T>> visitor = new SeqParser.Visitor<K, V, Result<T>>() {
            private int r = remaining;

            @Override
            public Result<T> success(@Nullable K value1, @Nullable V value2) {
                builder.put(value1, value2);
                r--;
                return r == 0 ? new Success<>(builder.build()) : null;
            }

            @Override
            public Result<T> partial(SeqParser<K, V> partial) {
                return (Partial<T>) buffer -> doParse(buffer, builder, r, partial);
            }
        };
        Result<T> result;
        while ((result = kvSeqParser.parse(buffer).accept(visitor)) == null) {
            kvSeqParser = this.elementParser;
        }
        return result;
    }
}
