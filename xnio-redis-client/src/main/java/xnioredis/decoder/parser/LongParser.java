package xnioredis.decoder.parser;

import java.nio.ByteBuffer;

public abstract class LongParser {
    static final LongParser PARSER = new LongParser() {
        @Override
        public Result parse(ByteBuffer buffer) {
            return doParse(buffer, false, 0, SIGN_OR_DIGIT);
        }
    };
    private static final LongParser.Visitor<Parser.Result<Integer>> INTEGER_ADAPTOR_VISITOR = new LongParser.Visitor<Parser.Result<Integer>>() {
        @Override
        public Parser.Result<Integer> success(long value) {
            return new Parser.Success<>((int) value);
        }

        @Override
        public Parser.Result<Integer> partial(LongParser partial) {
            return (Parser.Partial<Integer>) buffer -> partial.parse(buffer).accept(INTEGER_ADAPTOR_VISITOR);
        }
    };
    private static final LongParser.Visitor<Parser.Result<Long>> LONG_ADAPTOR_VISITOR = new LongParser.Visitor<Parser.Result<Long>>() {
        @Override
        public Parser.Result<Long> success(long value) {
            return new Parser.Success<>(value);
        }

        @Override
        public Parser.Result<Long> partial(LongParser partial) {
            return (Parser.Partial<Long>) buffer -> partial.parse(buffer).accept(LONG_ADAPTOR_VISITOR);
        }
    };
    public static final Parser<Integer> INTEGER_PARSER = buffer -> PARSER.parse(buffer).accept(INTEGER_ADAPTOR_VISITOR);
    public static final Parser<Long> LONG_PARSER = buffer -> PARSER.parse(buffer).accept(LONG_ADAPTOR_VISITOR);
    private static final int SIGN_OR_DIGIT = 0;
    private static final int DIGIT = 1;
    private static final int WAITING_FOR_LF = 2;

    abstract Result parse(ByteBuffer buffer);

    private static Result doParse(ByteBuffer buffer, boolean negative, long num, int state) {
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
                        return success(negative ? -num : num);
                    } else {
                        throw new IllegalStateException("LF is expected");
                    }
            }
        }
        return new LongPartial(negative, num, state);
    }

    interface Visitor<T> {
        T success(long value);

        T partial(LongParser partial);
    }

    interface Result {
        <T> T accept(Visitor<T> visitor);
    }

    private static class LongPartial extends LongParser implements Result {
        private final boolean negative;
        private final long num;
        private final int state;

        private LongPartial(boolean negative, long num, int state) {
            this.negative = negative;
            this.num = num;
            this.state = state;
        }

        @Override
        public Result parse(ByteBuffer buffer) {
            return doParse(buffer, negative, num, state);
        }

        @Override
        public <U> U accept(Visitor<U> visitor) {
            return visitor.partial(this);
        }
    }

    private static Result success(long result) {
        return new Result() {
            @Override
            public <T> T accept(Visitor<T> visitor) {
                return visitor.success(result);
            }
        };
    }
}
