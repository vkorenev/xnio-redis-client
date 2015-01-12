package xnioredis.decoder.parser;

import java.nio.ByteBuffer;

public class ErrorParser<T> implements ReplyParser<T> {
    private final Parser.Visitor<CharSequence, Result<T>> visitor = new Parser.Visitor<CharSequence, Result<T>>() {
        @Override
        public Result<T> success(CharSequence value) {
            return new Failure<>(value);
        }

        @Override
        public Result<T> partial(Parser<? extends CharSequence> partial) {
            return (Partial<T>) buffer -> partial.parse(buffer).accept(this);
        }
    };

    @Override
    public Result<T> parse(ByteBuffer buffer) {
        return StringParser.INSTANCE.parse(buffer).accept(visitor);
    }
}
