package xnioredis.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.IntFunction;

public class LenParser<T> implements Parser<T> {
    private final IntFunction<Parser<T>> bodyParserFactory;
    private final LongParser lengthParser;

    public LenParser(IntFunction<Parser<T>> bodyParserFactory) {
        this(bodyParserFactory, LongParser.PARSER);
    }

    private LenParser(IntFunction<Parser<T>> bodyParserFactory, LongParser lengthParser) {
        this.bodyParserFactory = bodyParserFactory;
        this.lengthParser = lengthParser;
    }

    @Override
    public <U> U parse(ByteBuffer buffer, Visitor<? super T, U> visitor, CharsetDecoder charsetDecoder) {
        return lengthParser.parse(buffer, new LongParser.Visitor<U>() {
            @Override
            public U success(long value) {
                if (value == -1) {
                    return visitor.success(null);
                } else {
                    return bodyParserFactory.apply((int) value).parse(buffer, visitor, charsetDecoder);
                }
            }

            @Override
            public U partial(LongParser partial) {
                return visitor.partial(new LenParser<>(bodyParserFactory, partial));
            }
        });
    }
}
