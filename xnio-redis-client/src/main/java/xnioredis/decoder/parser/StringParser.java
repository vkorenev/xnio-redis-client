package xnioredis.decoder.parser;

import java.nio.ByteBuffer;

public class StringParser implements Parser<CharSequence> {
    public static final Parser<CharSequence> INSTANCE = new StringParser();
    private static final int READING = 0;
    private static final int WAITING_FOR_LF = 1;

    private StringParser() {
    }

    public Result<CharSequence> parse(ByteBuffer buffer) {
        return doParse(buffer, new StringBuilder(), READING);
    }

    private static Result<CharSequence> doParse(ByteBuffer buffer, StringBuilder stringBuilder, int state) {
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            switch (state) {
                case READING:
                    if (b == '\r') {
                        state = WAITING_FOR_LF;
                    } else {
                        stringBuilder.append((char) b);
                    }
                    break;
                case WAITING_FOR_LF:
                    if (b == '\n') {
                        return new Success<>(stringBuilder);
                    } else {
                        throw new IllegalStateException("LF is expected");
                    }
            }
        }
        return new StringPartial(stringBuilder, state);
    }

    public static class StringPartial implements Parser.Partial<CharSequence> {
        private final StringBuilder stringBuilder;
        private final int state;

        public StringPartial(StringBuilder stringBuilder, int state) {
            this.stringBuilder = stringBuilder;
            this.state = state;
        }

        @Override
        public Result<CharSequence> parse(ByteBuffer buffer) {
            return doParse(buffer, stringBuilder, state);
        }
    }
}
