package xnioredis.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

public class PrefixedParser<T> implements Parser<T> {
    private final char marker;
    private final Parser<T> parser;

    public PrefixedParser(char marker, Parser<T> parser) {
        this.marker = marker;
        this.parser = parser;
    }

    @Override
    public <U> U parse(ByteBuffer buffer, Visitor<? super T, U> visitor, CharsetDecoder charsetDecoder) {
        if (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == marker) {
                return parser.parse(buffer, visitor, charsetDecoder);
            } else {
                throw new IllegalStateException('\'' + marker + "' is expected but '" + (char) b + "' was found");
            }
        } else {
            return visitor.partial(this);
        }
    }
}
