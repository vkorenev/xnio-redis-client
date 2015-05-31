package xnioredis;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.StreamConnection;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;
import xnioredis.decoder.parser.ReplyParser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.nio.charset.StandardCharsets.UTF_8;

class XnioRedisClient extends RedisClient {
    private final Pool<ByteBuffer> bufferPool;
    private final BlockingQueue<CommandWriter> writerQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<ReplyDecoder> decoderQueue = new LinkedBlockingQueue<>();
    private final CharsetEncoder charsetEncoder = UTF_8.newEncoder();
    private final StreamConnection connection;
    private final StreamSinkChannel outChannel;
    private final StreamSourceChannel inChannel;
    private ReplyDecoder currentDecoder;
    private CommandWriter currentWriter;

    public XnioRedisClient(StreamConnection connection, Pool<ByteBuffer> bufferPool) {
        this.connection = connection;
        this.bufferPool = bufferPool;
        this.outChannel = connection.getSinkChannel();
        this.inChannel = connection.getSourceChannel();
        this.inChannel.getReadSetter().set(inChannel -> {
            try (Pooled<ByteBuffer> pooledByteBuffer = bufferPool.allocate()) {
                ByteBuffer readBuffer = pooledByteBuffer.getResource();
                while (inChannel.read(readBuffer) > 0) {
                    readBuffer.flip();
                    try {
                        while (readBuffer.hasRemaining()) {
                            decoder().parse(readBuffer);
                        }
                    } finally {
                        readBuffer.clear();
                    }
                }
            } catch (Throwable e) {
                decoder().fail(e);
            }
        });
        this.inChannel.resumeReads();
        this.outChannel.getWriteSetter().set(new ChannelListener<StreamSinkChannel>() {
            @Override
            public void handleEvent(StreamSinkChannel outChannel) {
                CommandWriter commandWriter;
                while ((commandWriter = writer()) != null) {
                    try {
                        if (commandWriter.write(outChannel, charsetEncoder, XnioRedisClient.this.bufferPool)) {
                            currentWriter = null;
                        } else {
                            return;
                        }
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                }
                outChannel.suspendWrites();
            }
        });
    }

    private ReplyDecoder decoder() {
        if (currentDecoder == null) {
            currentDecoder = decoderQueue.poll();
            if (currentDecoder == null) {
                currentDecoder = new ReplyDecoder() {
                    @Override
                    public void parse(ByteBuffer buffer) throws IOException {
                        int len = buffer.remaining();
                        byte[] bytes = new byte[len];
                        buffer.get(bytes);
                        throw new IllegalStateException("Unexpected input: " + Arrays.toString(bytes));
                    }

                    @Override
                    public void fail(Throwable e) {
                        throw Throwables.propagate(e);
                    }
                };
            }
        }
        return currentDecoder;
    }

    private CommandWriter writer() {
        if (currentWriter == null) {
            currentWriter = writerQueue.poll();
        }
        return currentWriter;
    }

    @Override
    public <T> ListenableFuture<T> send(Command<T> command, boolean autoFlush) {
        SettableFuture<T> future = SettableFuture.create();
        decoderQueue.add(new ReplyDecoder() {
            private ReplyParser<? extends T> parser = command.parser();

            @Override
            public void parse(ByteBuffer buffer) throws IOException {
                parser.parseReply(buffer, new ReplyParser.ReplyVisitor<T, Void>() {
                    @Override
                    public Void success(@Nullable T value) {
                        setReply(value);
                        return null;
                    }

                    @Override
                    public Void failure(CharSequence message) {
                        fail(new RedisException(message.toString()));
                        return null;
                    }

                    @Override
                    public Void partialReply(ReplyParser<? extends T> partial) {
                        parser = partial;
                        return null;
                    }
                });
            }

            private void setReply(@Nullable T reply) {
                future.set(reply);
                currentDecoder = null;
            }

            @Override
            public void fail(Throwable e) {
                future.setException(e);
                currentDecoder = null;
            }
        });
        writerQueue.add(command.writer());
        outChannel.resumeWrites();
        return future;
    }

    @Override
    public ListenableFuture<Void> flush() {
        SettableFuture<Void> future = SettableFuture.create();
        outChannel.getIoThread().execute(() -> {
            try {
                outChannel.flush();
                future.set(null);
            } catch (IOException e) {
                future.setException(e);
            }
        });
        return future;
    }

    @Override
    public void close() {
        IoUtils.safeClose(inChannel);
        IoUtils.safeClose(outChannel);
        IoUtils.safeClose(connection);
    }
}
