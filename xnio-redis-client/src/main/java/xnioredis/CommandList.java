package xnioredis;

import org.xnio.Pool;
import org.xnio.channels.StreamSinkChannel;
import xnioredis.decoder.parser.ReplyParser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CommandList<T> implements Command<List<T>> {
    private final List<Command<T>> commands;

    public CommandList(List<Command<T>> commands) {
        this.commands = commands;
    }

    @Override
    public void writeCommand(StreamSinkChannel channel, CharsetEncoder charsetEncoder, Pool<ByteBuffer> bufferPool) throws IOException {
        for (Command<T> command : commands) {
            command.writeCommand(channel, charsetEncoder, bufferPool);
        }
    }

    @Override
    public ReplyParser<List<T>> parser() {
        return new ReplyParser<List<T>>() {
            private final List<T> replies = new ArrayList<>(commands.size());
            private final Iterator<Command<T>> commandIterator = commands.iterator();

            @Override
            public <U> U parseReply(ByteBuffer buffer, ReplyVisitor<? super List<T>, U> visitor) {
                if (commandIterator.hasNext()) {
                    return doParse(buffer, visitor, commandIterator.next().parser());
                } else {
                    return visitor.success(replies);
                }
            }

            private <U> U doParse(ByteBuffer buffer, ReplyVisitor<? super List<T>, U> visitor, ReplyParser<? extends T> parser) {
                return parser.parseReply(buffer, new ReplyVisitor<T, U>() {
                    @Override
                    public U failure(CharSequence message) {
                        return visitor.failure(message);
                    }

                    @Override
                    public U partialReply(ReplyParser<? extends T> partial) {
                        return visitor.partialReply(new ReplyParser<List<T>>() {
                            @Override
                            public <U1> U1 parseReply(ByteBuffer buffer, ReplyVisitor<? super List<T>, U1> visitor) {
                                return doParse(buffer, visitor, partial);
                            }
                        });
                    }

                    @Override
                    public U success(@Nullable T value) {
                        replies.add(value);
                        return parseReply(buffer, visitor);
                    }
                });
            }
        };
    }
}
