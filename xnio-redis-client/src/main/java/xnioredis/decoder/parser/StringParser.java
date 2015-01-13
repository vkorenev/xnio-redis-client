package xnioredis.decoder.parser;

import java.nio.ByteBuffer;

public class StringParser implements Parser<CharSequence> {
    public static final Parser<CharSequence> INSTANCE = new StringParser();
    private static final int READING = 0;
    private static final int WAITING_FOR_LF = 1;

    private StringParser() {
    }

    @Override
    public <U> U parse(ByteBuffer buffer, Visitor<? super CharSequence, U> visitor) {
        return doParse(buffer, visitor, new StringBuilder(), READING);
    }

    private static <U> U doParse(ByteBuffer buffer, Visitor<? super CharSequence, U> visitor, StringBuilder stringBuilder, int state) {
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
                        return visitor.success(stringBuilder);
                    } else {
                        throw new IllegalStateException("LF is expected");
                    }
            }
        }
        int state1 = state;
        return visitor.partial(new Parser<CharSequence>() {
            @Override
            public <U1> U1 parse(ByteBuffer buffer, Visitor<? super CharSequence, U1> visitor) {
                return doParse(buffer, visitor, stringBuilder, state1);
            }
        });
    }
}
