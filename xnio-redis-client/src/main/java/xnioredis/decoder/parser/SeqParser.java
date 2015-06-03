package xnioredis.decoder.parser;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Objects;
import java.util.function.BiFunction;

public interface SeqParser<T1, T2> {
    static <T1, T2> SeqParser<T1, T2> seq(Parser<T1> parser1, Parser<T2> parser2) {
        return new SeqParser<T1, T2>() {
            @Override
            public <R> R parse(ByteBuffer buffer, Visitor<? super T1, ? super T2, R> visitor,
                    CharsetDecoder charsetDecoder) {
                return doParse1(parser1, buffer, visitor, charsetDecoder);
            }

            private <R> R doParse1(Parser<? extends T1> parser1, ByteBuffer buffer,
                    Visitor<? super T1, ? super T2, R> visitor, CharsetDecoder charsetDecoder) {
                return parser1.parse(buffer, new Parser.Visitor<T1, R>() {
                    @Override
                    public R success(@Nullable T1 value1) {
                        return doParse2(parser2, buffer, visitor, value1, charsetDecoder);
                    }

                    private <R1> R1 doParse2(Parser<? extends T2> parser2, ByteBuffer buffer,
                            Visitor<? super T1, ? super T2, R1> visitor, @Nullable T1 value1,
                            CharsetDecoder charsetDecoder) {
                        return parser2.parse(buffer, new Parser.Visitor<T2, R1>() {
                            @Override
                            public R1 success(@Nullable T2 value2) {
                                return visitor.success(value1, value2);
                            }

                            @Override
                            public R1 partial(Parser<? extends T2> partial) {
                                return visitor.partial(new SeqParser<T1, T2>() {
                                    @Override
                                    public <R2> R2 parse(ByteBuffer buffer, Visitor<? super T1, ? super T2, R2> visitor,
                                            CharsetDecoder charsetDecoder) {
                                        return doParse2(partial, buffer, visitor, value1, charsetDecoder);
                                    }
                                });
                            }
                        }, charsetDecoder);
                    }

                    @Override
                    public R partial(Parser<? extends T1> partial) {
                        return visitor.partial(new SeqParser<T1, T2>() {
                            @Override
                            public <R1> R1 parse(ByteBuffer buffer, Visitor<? super T1, ? super T2, R1> visitor,
                                    CharsetDecoder charsetDecoder) {
                                return doParse1(partial, buffer, visitor, charsetDecoder);
                            }
                        });
                    }
                }, charsetDecoder);
            }
        };
    }

    <R> R parse(ByteBuffer buffer, Visitor<? super T1, ? super T2, R> visitor, CharsetDecoder charsetDecoder);

    default <R> Parser<R> mapToParser(BiFunction<? super T1, ? super T2, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return new Parser<R>() {
            @Override
            public <U> U parse(ByteBuffer buffer, Visitor<? super R, U> visitor, CharsetDecoder charsetDecoder) {
                return SeqParser.this.parse(buffer, new SeqParser.Visitor<T1, T2, U>() {
                    @Override
                    public U success(@Nullable T1 value1, @Nullable T2 value2) {
                        return visitor.success(mapper.apply(value1, value2));
                    }

                    @Override
                    public U partial(SeqParser<? extends T1, ? extends T2> partial) {
                        return visitor.partial(partial.mapToParser(mapper));
                    }
                }, charsetDecoder);
            }
        };
    }

    interface Visitor<T1, T2, R> {
        R success(@Nullable T1 value1, @Nullable T2 value2);

        R partial(SeqParser<? extends T1, ? extends T2> partial);
    }
}
