package xnioredis.decoder.parser;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.BiFunction;

public interface SeqParser<T1, T2> {
    static <T1, T2> SeqParser<T1, T2> seq(Parser<T1> parser1, Parser<T2> parser2) {
        return buffer -> parser1.parse(buffer).accept(new ResultVisitor1<>(parser2, buffer));
    }

    Result<T1, T2> parse(ByteBuffer buffer);

    default <R> Parser<R> mapToParser(BiFunction<? super T1, ? super T2, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return buffer -> {
            Result<T1, T2> result = SeqParser.this.parse(buffer);
            return new Parser.Result<R>() {
                @Override
                public <U> U accept(Parser.Visitor<? super R, U> visitor) {
                    return result.accept(new Visitor<T1, T2, U>() {
                        @Override
                        public U success(@Nullable T1 value1, @Nullable T2 value2) {
                            return visitor.success(mapper.apply(value1, value2));
                        }

                        @Override
                        public U partial(SeqParser<T1, T2> partial) {
                            return visitor.partial(partial.mapToParser(mapper));
                        }
                    });
                }
            };
        };
    }

    static interface Result<T1, T2> {
        <R> R accept(Visitor<T1, T2, R> visitor);
    }

    static interface Visitor<T1, T2, R> {
        R success(@Nullable T1 value1, @Nullable T2 value2);

        R partial(SeqParser<T1, T2> partial);
    }

    static interface Partial<T1, T2> extends SeqParser<T1, T2>, Result<T1, T2> {
        @Override
        default <R> R accept(Visitor<T1, T2, R> visitor) {
            return visitor.partial(this);
        }
    }

    static class ResultVisitor1<T1, T2> implements Parser.Visitor<T1, Result<T1, T2>> {
        private final Parser<T2> parser2;
        private final ByteBuffer buffer;

        public ResultVisitor1(Parser<T2> parser2, ByteBuffer buffer) {
            this.parser2 = parser2;
            this.buffer = buffer;
        }

        @Override
        public Result<T1, T2> success(@Nullable T1 value1) {
            return parser2.parse(buffer).accept(new Parser.Visitor<T2, Result<T1, T2>>() {
                @Override
                public Result<T1, T2> success(@Nullable T2 value2) {
                    return new Result<T1, T2>() {
                        @Override
                        public <R> R accept(Visitor<T1, T2, R> visitor) {
                            return visitor.success(value1, value2);
                        }
                    };
                }

                @Override
                public Result<T1, T2> partial(Parser<? extends T2> partial) {
                    return (Partial<T1, T2>) buffer -> partial.parse(buffer).accept(this);
                }
            });
        }

        @Override
        public Result<T1, T2> partial(Parser<? extends T1> partial) {
            return (Partial<T1, T2>) buffer -> partial.parse(buffer).accept(new ResultVisitor1<T1, T2>(parser2, buffer));
        }
    }
}
