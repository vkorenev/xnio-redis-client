package xnioredis;

import xnioredis.decoder.parser.ReplyParser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CommandList<T> implements Command<List<T>> {
    private final List<Command<T>> commands;

    public CommandList(List<Command<T>> commands) {
        this.commands = commands;
    }

    @Override
    public CommandWriter writer() {
        return new CommandWriter() {
            private final List<CommandWriter> writers = commands.stream().map(Command::writer).collect(Collectors.toList());

            @Override
            public void write(Supplier<ByteBuffer> writeBufferSupplier, CharsetEncoder charsetEncoder) throws IOException {
                for (CommandWriter writer : writers) {
                    writer.write(writeBufferSupplier, charsetEncoder);
                }
            }
        };
    }

    @Override
    public ReplyParser<List<T>> parser() {
        return new ReplyParser<List<T>>() {
            private final List<T> replies = new ArrayList<>(commands.size());
            private final Iterator<Command<T>> commandIterator = commands.iterator();

            @Override
            public <U> U parseReply(ByteBuffer buffer, ReplyVisitor<? super List<T>, U> visitor,
                    CharsetDecoder charsetDecoder) {
                if (commandIterator.hasNext()) {
                    return doParse(buffer, visitor, commandIterator.next().parser(), charsetDecoder);
                } else {
                    return visitor.success(replies);
                }
            }

            private <U> U doParse(ByteBuffer buffer, ReplyVisitor<? super List<T>, U> visitor,
                    ReplyParser<? extends T> parser, CharsetDecoder charsetDecoder) {
                return parser.parseReply(buffer, new ReplyVisitor<T, U>() {
                    @Override
                    public U failure(CharSequence message) {
                        return visitor.failure(message);
                    }

                    @Override
                    public U partialReply(ReplyParser<? extends T> partial) {
                        return visitor.partialReply(new ReplyParser<List<T>>() {
                            @Override
                            public <U1> U1 parseReply(ByteBuffer buffer, ReplyVisitor<? super List<T>, U1> visitor,
                                    CharsetDecoder charsetDecoder) {
                                return doParse(buffer, visitor, partial, charsetDecoder);
                            }
                        });
                    }

                    @Override
                    public U success(@Nullable T value) {
                        replies.add(value);
                        return parseReply(buffer, visitor, charsetDecoder);
                    }
                }, charsetDecoder);
            }
        };
    }
}
