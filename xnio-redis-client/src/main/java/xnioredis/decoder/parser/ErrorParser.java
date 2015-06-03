package xnioredis.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

public class ErrorParser<T> implements ReplyParser<T> {

    private final Parser<? extends CharSequence> errorParser;

    private ErrorParser(Parser<? extends CharSequence> errorParser) {
        this.errorParser = errorParser;
    }

    public ErrorParser() {
        this(StringParser.INSTANCE);
    }

    @Override
    public <U> U parseReply(ByteBuffer buffer, ReplyVisitor<? super T, U> visitor, CharsetDecoder charsetDecoder) {
        return doParse(buffer, visitor, errorParser, charsetDecoder);
    }

    private <U> U doParse(ByteBuffer buffer, ReplyVisitor<? super T, U> visitor,
            Parser<? extends CharSequence> errorParser, CharsetDecoder charsetDecoder) {
        Parser.Visitor<CharSequence, U> visitor1 = new Parser.Visitor<CharSequence, U>() {
            @Override
            public U success(CharSequence message) {
                return visitor.failure(message);
            }

            @Override
            public U partial(Parser<? extends CharSequence> partial) {
                return visitor.partialReply(new ErrorParser<T>(partial));
            }
        };
        return errorParser.parse(buffer, visitor1, charsetDecoder);
    }
}
