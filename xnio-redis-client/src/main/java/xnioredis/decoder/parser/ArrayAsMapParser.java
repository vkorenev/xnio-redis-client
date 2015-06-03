package xnioredis.decoder.parser;

import xnioredis.decoder.MapBuilderFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

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
    public <U> U parse(ByteBuffer buffer, Visitor<? super T, U> visitor, CharsetDecoder charsetDecoder) {
        return doParse(buffer, visitor, builderFactory.create(len), len, kvParser, charsetDecoder);
    }

    private <U> U doParse(ByteBuffer buffer, Visitor<? super T, U> visitor,
            MapBuilderFactory.Builder<K, V, ? extends T> builder, int remaining,
            SeqParser<? extends K, ? extends V> kvSeqParser, CharsetDecoder charsetDecoder) {
        while (remaining > 0) {
            Parser<T> partial = parsePartial(buffer, builder, remaining, kvSeqParser, charsetDecoder);
            if (partial != null) {
                return visitor.partial(partial);
            } else {
                remaining--;
                kvSeqParser = kvParser;
            }
        }
        return visitor.success(builder.build());
    }

    private Parser<T> parsePartial(ByteBuffer buffer, MapBuilderFactory.Builder<K, V, ? extends T> builder,
            int remaining, SeqParser<? extends K, ? extends V> kvSeqParser, CharsetDecoder charsetDecoder) {
        return kvSeqParser.parse(buffer, new SeqParser.Visitor<K, V, Parser<T>>() {
            @Override
            public Parser<T> success(@Nullable K value1, @Nullable V value2) {
                builder.put(value1, value2);
                return null;
            }

            @Override
            public Parser<T> partial(SeqParser<? extends K, ? extends V> partial) {
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
