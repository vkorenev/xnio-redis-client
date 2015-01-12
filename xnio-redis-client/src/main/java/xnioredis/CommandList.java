package xnioredis;

import org.xnio.Pool;
import org.xnio.channels.StreamSinkChannel;
import xnioredis.decoder.parser.Parser;
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
            public Result<List<T>> parse(ByteBuffer buffer) {
                if (commandIterator.hasNext()) {
                    return doParse(buffer, commandIterator.next().parser());
                } else {
                    return new Parser.Success<>(replies);
                }
            }

            private Result<List<T>> doParse(ByteBuffer buffer, ReplyParser<? extends T> parser) {
                return parser.parse(buffer).acceptReply(new ReplyVisitor<T, Result<List<T>>>() {
                    @Override
                    public Result<List<T>> failure(CharSequence message) {
                        return new Failure<>(message);
                    }

                    @Override
                    public Result<List<T>> partialReply(ReplyParser<? extends T> partial) {
                        return (Partial<List<T>>) buffer -> doParse(buffer, partial);
                    }

                    @Override
                    public Result<List<T>> success(@Nullable T value) {
                        replies.add(value);
                        return parse(buffer);
                    }
                });
            }
        };
    }
}
