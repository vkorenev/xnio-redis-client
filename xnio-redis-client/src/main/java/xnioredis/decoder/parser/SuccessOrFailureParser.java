package xnioredis.decoder.parser;

import java.nio.ByteBuffer;

public class SuccessOrFailureParser<T> implements ReplyParser.Partial<T> {
    private final ErrorParser<T> errorParser = new ErrorParser<>();
    private final char marker;
    private final Parser<T> parser;

    public SuccessOrFailureParser(char marker, Parser<T> parser) {
        this.marker = marker;
        this.parser = parser;
    }

    @Override
    public Result<T> parse(ByteBuffer buffer) {
        if (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == marker) {
                return parser.parse(buffer);
            } else if (b == '-') {
                return errorParser.parse(buffer);
            } else {
                throw new IllegalStateException('\'' + marker + "' is expected but '" + (char) b + "' was found");
            }
        }
        return this;
    }
}
