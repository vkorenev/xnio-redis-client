package xnioredis.decoder.parser;

import java.nio.ByteBuffer;

public class SuccessOrFailureParser<T> implements ReplyParser<T> {
    private final ErrorParser<T> errorParser = new ErrorParser<>();
    private final char marker;
    private final Parser<T> parser;

    public SuccessOrFailureParser(char marker, Parser<T> parser) {
        this.marker = marker;
        this.parser = parser;
    }

    @Override
    public <U> U parseReply(ByteBuffer buffer, ReplyVisitor<? super T, U> visitor) {
        if (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == marker) {
                return parser.parse(buffer, visitor);
            } else if (b == '-') {
                return errorParser.parseReply(buffer, visitor);
            } else {
                throw new IllegalStateException("'" + marker + "' is expected but '" + (char) b + "' was found");
            }
        } else {
            return visitor.partialReply(this);
        }
    }
}
