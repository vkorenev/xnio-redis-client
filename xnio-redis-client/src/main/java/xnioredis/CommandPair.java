package xnioredis;

import org.xnio.Pool;
import org.xnio.channels.StreamSinkChannel;
import xnioredis.decoder.parser.ReplyParser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.function.BiFunction;

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
            public boolean write(StreamSinkChannel channel, CharsetEncoder charsetEncoder, Pool<ByteBuffer> bufferPool) throws IOException {
                return commandWriter1.write(channel, charsetEncoder, bufferPool) &&
                        commandWriter2.write(channel, charsetEncoder, bufferPool);
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

        public ReplyParser1(ReplyParser<? extends T1> parser1, ReplyParser<? extends T2> parser2, BiFunction<T1, T2, R> biFunction) {
            this.parser1 = parser1;
            this.parser2 = parser2;
            this.biFunction = biFunction;
        }

        @Override
        public <U> U parseReply(ByteBuffer buffer, ReplyVisitor<? super R, U> visitor) {
            return parser1.parseReply(buffer, new ReplyVisitor<T1, U>() {
                @Override
                public U failure(CharSequence message) {
                    return visitor.failure(message);
                }

                @Override
                public U partialReply(ReplyParser<? extends T1> partial) {
                    return visitor.partialReply(new ReplyParser1<>(partial, parser2, biFunction));
                }

                @Override
                public U success(@Nullable T1 value1) {
                    return parse2(buffer, visitor, value1, parser2);
                }

                private <U1> U1 parse2(ByteBuffer buffer, ReplyVisitor<? super R, U1> visitor, @Nullable T1 value1, ReplyParser<? extends T2> parser2) {
                    return parser2.parseReply(buffer, new ReplyVisitor<T2, U1>() {
                        @Override
                        public U1 failure(CharSequence message) {
                            return visitor.failure(message);
                        }

                        @Override
                        public U1 partialReply(ReplyParser<? extends T2> partial) {
                            return visitor.partialReply(new ReplyParser<R>() {
                                @Override
                                public <U2> U2 parseReply(ByteBuffer buffer, ReplyVisitor<? super R, U2> visitor) {
                                    return parse2(buffer, visitor, value1, partial);
                                }
                            });
                        }

                        @Override
                        public U1 success(@Nullable T2 value2) {
                            return visitor.success(biFunction.apply(value1, value2));
                        }
                    });
                }
            });
        }
    }
}
