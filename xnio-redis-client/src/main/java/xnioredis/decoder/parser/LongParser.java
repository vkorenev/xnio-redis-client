package xnioredis.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.LongFunction;

public abstract class LongParser {
    static final LongParser PARSER = new LongParser() {
        @Override
        <T> T parse(ByteBuffer buffer, Visitor<T> visitor) {
            return doParse(buffer, visitor, false, 0, SIGN_OR_DIGIT);
        }
    };
    public static final Parser<Integer> INTEGER_PARSER = new ParserAdaptor<>(PARSER, l -> (int) l);
    public static final Parser<Long> LONG_PARSER = new ParserAdaptor<>(PARSER, l -> l);
    private static final int SIGN_OR_DIGIT = 0;
    private static final int DIGIT = 1;
    private static final int WAITING_FOR_LF = 2;

    abstract <T> T parse(ByteBuffer buffer, Visitor<T> visitor);

    private static <T> T doParse(ByteBuffer buffer, Visitor<T> visitor, boolean negative, long num, int state) {
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            switch (state) {
                case SIGN_OR_DIGIT:
                    state = DIGIT;
                    if (b == '-') {
                        negative = true;
                        break;
                    }
                case DIGIT:
                    switch (b) {
                        case '0':
                            num *= 10;
                            break;
                        case '1':
                            num = num * 10 + 1;
                            break;
                        case '2':
                            num = num * 10 + 2;
                            break;
                        case '3':
                            num = num * 10 + 3;
                            break;
                        case '4':
                            num = num * 10 + 4;
                            break;
                        case '5':
                            num = num * 10 + 5;
                            break;
                        case '6':
                            num = num * 10 + 6;
                            break;
                        case '7':
                            num = num * 10 + 7;
                            break;
                        case '8':
                            num = num * 10 + 8;
                            break;
                        case '9':
                            num = num * 10 + 9;
                            break;
                        case '\r':
                            state = WAITING_FOR_LF;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected character: " + (char) b);
                    }
                    break;
                case WAITING_FOR_LF:
                    if (b == '\n') {
                        return visitor.success(negative ? -num : num);
                    } else {
                        throw new IllegalStateException("LF is expected");
                    }
            }
        }
        return visitor.partial(new LongPartial(negative, num, state));
    }

    interface Visitor<T> {
        T success(long value);

        T partial(LongParser partial);
    }

    private static class LongPartial extends LongParser {
        private final boolean negative;
        private final long num;
        private final int state;

        private LongPartial(boolean negative, long num, int state) {
            this.negative = negative;
            this.num = num;
            this.state = state;
        }

        @Override
        <T> T parse(ByteBuffer buffer, Visitor<T> visitor) {
            return doParse(buffer, visitor, negative, num, state);
        }
    }

    private static class ParserAdaptor<T> implements Parser<T> {
        private final LongParser parser;
        private final LongFunction<T> longFunction;

        private ParserAdaptor(LongParser parser, LongFunction<T> longFunction) {
            this.parser = parser;
            this.longFunction = longFunction;
        }

        @Override
        public <U> U parse(ByteBuffer buffer, Visitor<? super T, U> visitor, CharsetDecoder charsetDecoder) {
            return parser.parse(buffer, new LongParser.Visitor<U>() {
                @Override
                public U success(long value) {
                    return visitor.success(longFunction.apply(value));
                }

                @Override
                public U partial(LongParser partial) {
                    return visitor.partial(new ParserAdaptor<>(partial, longFunction));
                }
            });
        }
    }
}
