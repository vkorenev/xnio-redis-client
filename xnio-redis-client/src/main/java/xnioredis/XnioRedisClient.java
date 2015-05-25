package xnioredis;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.IoUtils;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.channels.Channels;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;
import xnioredis.decoder.parser.ReplyParser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static java.nio.charset.StandardCharsets.UTF_8;

class XnioRedisClient extends RedisClient {
    private static final Pool<ByteBuffer> BUFFER_POOL = new ByteBufferSlicePool(BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR, 4096, 4096 * 256);
    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private final BlockingQueue<ReplyDecoder> queue = new LinkedBlockingQueue<>();
    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(4096);
    private final CharsetEncoder charsetEncoder = UTF_8.newEncoder();
    private final StreamConnection connection;
    private final StreamSinkChannel outChannel;
    private final StreamSourceChannel inChannel;
    private ReplyDecoder currentDecoder;

    public XnioRedisClient(StreamConnection connection) {
        this.connection = connection;
        this.outChannel = connection.getSinkChannel();
        this.inChannel = connection.getSourceChannel();
        this.inChannel.getReadSetter().set(inChannel -> {
            try {
                while (inChannel.read(readBuffer) > 0) {
                    readBuffer.flip();
                    while (readBuffer.hasRemaining()) {
                        decoder().parse(readBuffer);
                    }
                    readBuffer.clear();
                }
            } catch (Exception e) {
                decoder().fail(e);
            }
        });
        this.inChannel.resumeReads();
    }

    private ReplyDecoder decoder() {
        if (currentDecoder == null) {
            currentDecoder = queue.poll();
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
                    public void fail(Exception e) {
                        throw Throwables.propagate(e);
                    }
                };
            }
        }
        return currentDecoder;
    }

    @Override
    public <T> ListenableFuture<T> send(Command<T> command, boolean autoFlush) {
        SettableFuture<T> future = SettableFuture.create();
        executorService.execute(() -> {
            try {
                queue.add(new ReplyDecoder() {
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
                    public void fail(Exception e) {
                        future.setException(e);
                        currentDecoder = null;
                    }
                });
                command.writeCommand(outChannel, charsetEncoder, BUFFER_POOL);
                if (autoFlush) {
                    Channels.flushBlocking(outChannel);
                }
            } catch (IOException e) {
                future.setException(e);
            }
        });
        return future;
    }

    @Override
    public void flush() throws IOException {
        outChannel.flush();
    }

    @Override
    public void close() {
        IoUtils.safeClose(inChannel);
        IoUtils.safeClose(outChannel);
        IoUtils.safeClose(connection);
        executorService.shutdown();
    }
}
