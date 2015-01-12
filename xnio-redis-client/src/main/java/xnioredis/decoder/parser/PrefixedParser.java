package xnioredis.decoder.parser;

import java.nio.ByteBuffer;

public class PrefixedParser<T> implements Parser.Partial<T> {
    private final char marker;
    private final Parser<T> parser;

    public PrefixedParser(char marker, Parser<T> parser) {
        this.marker = marker;
        this.parser = parser;
    }

    @Override
    public Result<T> parse(ByteBuffer buffer) {
        if (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == marker) {
                return parser.parse(buffer);
            } else {
                throw new IllegalStateException('\'' + marker + "' is expected but '" + (char) b + "' was found");
            }
        }
        return this;
    }
}
