package xnioredis.decoder.parser;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

public class LenParser<T> implements Parser<T> {
    private final IntFunction<Parser<T>> bodyParserFactory;

    public LenParser(IntFunction<Parser<T>> bodyParserFactory) {
        this.bodyParserFactory = bodyParserFactory;
    }

    @Override
    public Result<T> parse(ByteBuffer buffer) {
        return doParse(buffer, LongParser.PARSER);
    }

    private Result<T> doParse(ByteBuffer buffer, LongParser lengthParser) {
        return lengthParser.parse(buffer).accept(new LongParser.Visitor<Result<T>>() {
            @Override
            public Result<T> success(long value) {
                if (value == -1) {
                    return new Success<>(null);
                } else {
                    return bodyParserFactory.apply((int) value).parse(buffer);
                }
            }

            @Override
            public Result<T> partial(LongParser partial) {
                return (Partial<T>) buffer -> doParse(buffer, partial);
            }
        });
    }
}
