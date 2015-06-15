package xnioredis;

import xnioredis.decoder.parser.ReplyParser;
import xnioredis.encoder.RespSink;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CommandPair<T1, T2, R> implements Command<R> {

    private final Command<T1> command1;
    private final Command<T2> command2;
    private final BiFunction<T1, T2, R> biFunction;

    public CommandPair(Command<T1> command1, Command<T2> command2, BiFunction<T1, T2, R> biFunction) {
        this.command1 = command1;
        this.command2 = command2;
        this.biFunction = biFunction;
    }

    @Override
    public CommandWriter writer() {
        return new CommandWriter() {
            private final CommandWriter commandWriter1 = command1.writer();
            private final CommandWriter commandWriter2 = command2.writer();

            @Override
            public void write(RespSink sink) throws IOException {
                commandWriter1.write(sink);
                commandWriter2.write(sink);
            }
        };
    }

    @Override
    public ReplyParser<R> parser() {
        return new ReplyParser1<>(command1.parser(), command2.parser(), biFunction);
    }

    private static class ReplyParser1<T1, T2, R> implements ReplyParser<R> {
        private final ReplyParser<? extends T1> parser1;
        private final ReplyParser<? extends T2> parser2;
        private final BiFunction<T1, T2, R> biFunction;

        public ReplyParser1(ReplyParser<? extends T1> parser1, ReplyParser<? extends T2> parser2,
                BiFunction<T1, T2, R> biFunction) {
            this.parser1 = parser1;
            this.parser2 = parser2;
            this.biFunction = biFunction;
        }

        @Override
        public <U> U parseReply(ByteBuffer buffer, Function<? super R, U> resultHandler,
                PartialReplyHandler<? super R, U> partialReplyHandler, FailureHandler<U> failureHandler,
                CharsetDecoder charsetDecoder) {
            return parser1.parseReply(buffer, new Function<T1, U>() {
                        @Override
                        public U apply(@Nullable T1 value1) {
                            return parse2(buffer, resultHandler, partialReplyHandler, failureHandler, value1, parser2);
                        }

                        private <U1> U1 parse2(ByteBuffer buffer, Function<? super R, U1> resultHandler,
                                PartialReplyHandler<? super R, U1> partialReplyHandler,
                                FailureHandler<U1> failureHandler, @Nullable T1 value1,
                                ReplyParser<? extends T2> parser2) {
                            return parser2
                                    .parseReply(buffer, value2 -> resultHandler.apply(biFunction.apply(value1, value2)),
                                            partial -> partialReplyHandler.partialReply(new ReplyParser<R>() {
                                                @Override
                                                public <U2> U2 parseReply(ByteBuffer buffer,
                                                        Function<? super R, U2> resultHandler,
                                                        PartialReplyHandler<? super R, U2> partialReplyHandler,
                                                        FailureHandler<U2> failureHandler,
                                                        CharsetDecoder charsetDecoder) {
                                                    return parse2(buffer, resultHandler, partialReplyHandler,
                                                            failureHandler, value1, partial);
                                                }
                                            }), failureHandler, charsetDecoder);
                        }
                    }, partial -> partialReplyHandler.partialReply(new ReplyParser1<>(partial, parser2, biFunction)),
                    failureHandler, charsetDecoder);
        }
    }
}
