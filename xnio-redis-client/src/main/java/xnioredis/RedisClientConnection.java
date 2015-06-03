package xnioredis;

import com.google.common.base.Throwables;
import org.xnio.IoUtils;
import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.StreamConnection;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class RedisClientConnection implements AutoCloseable {
    private final BlockingQueue<ReplyDecoder> decoderQueue = new LinkedBlockingQueue<>();
    private final StreamConnection connection;
    private final StreamSinkChannel outChannel;
    private final StreamSourceChannel inChannel;
    private ReplyDecoder currentDecoder;

    RedisClientConnection(StreamConnection connection, Pool<ByteBuffer> bufferPool, Charset charset,
            BlockingQueue<CommandEncoderDecoder> commandsQueue) {
        this.connection = connection;
        CharsetDecoder charsetDecoder = charset.newDecoder();
        this.inChannel = connection.getSourceChannel();
        this.inChannel.getReadSetter().set(inChannel -> {
            try (Pooled<ByteBuffer> pooledByteBuffer = bufferPool.allocate()) {
                ByteBuffer readBuffer = pooledByteBuffer.getResource();
                while (inChannel.read(readBuffer) > 0) {
                    readBuffer.flip();
                    try {
                        while (readBuffer.hasRemaining()) {
                            if (decoder().parse(readBuffer, charsetDecoder)) {
                                currentDecoder = null;
                            }
                        }
                    } finally {
                        readBuffer.clear();
                    }
                }
            } catch (Throwable e) {
                if (currentDecoder != null) {
                    currentDecoder.fail(e);
                }
                decoderQueue.forEach(decoder -> decoder.fail(e));
            }
        });
        this.inChannel.resumeReads();
        ByteBufferBundle byteBufferBundle = new ByteBufferBundle(bufferPool);
        CharsetEncoder charsetEncoder = charset.newEncoder();
        this.outChannel = connection.getSinkChannel();
        this.outChannel.getWriteSetter().set(outChannel -> {
            try {
                while (!commandsQueue.isEmpty() || !byteBufferBundle.isEmpty()) {
                    CommandEncoderDecoder command;
                    while (byteBufferBundle.allocSize() <= 1 && (command = commandsQueue.poll()) != null) {
                        decoderQueue.add(command);
                        command.writer().write(byteBufferBundle, charsetEncoder);
                    }
                    byteBufferBundle.startReading();
                    try {
                        long bytesWritten = outChannel.write(byteBufferBundle.getReadBuffers());
                        if (bytesWritten == 0) {
                            return;
                        }
                    } finally {
                        byteBufferBundle.startWriting();
                    }
                }
            } catch (IOException e) {
                if (currentDecoder != null) {
                    currentDecoder.fail(e);
                }
                decoderQueue.forEach(decoder -> decoder.fail(e));
            }
            outChannel.suspendWrites();
        });
    }

    private ReplyDecoder decoder() {
        if (currentDecoder == null) {
            currentDecoder = decoderQueue.poll();
            if (currentDecoder == null) {
                currentDecoder = new ReplyDecoder() {
                    @Override
                    public boolean parse(ByteBuffer buffer, CharsetDecoder charsetDecoder) throws IOException {
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

    void commandAdded() {
        outChannel.resumeWrites();
    }

    @Override
    public void close() {
        IoUtils.safeClose(inChannel);
        IoUtils.safeClose(outChannel);
        IoUtils.safeClose(connection);
    }

    interface ReplyDecoder {
        boolean parse(ByteBuffer buffer, CharsetDecoder charsetDecoder) throws IOException;

        void fail(Throwable e);
    }

    interface CommandEncoderDecoder extends ReplyDecoder {
        CommandWriter writer();
    }
}
