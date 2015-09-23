package xnioredis;

import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import xnioredis.RedisClientConnection.CommandEncoderDecoder;
import xnioredis.decoder.parser.ReplyParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class XnioRedisClient<F, SF extends F> implements AutoCloseable {
    private final BlockingQueue<CommandEncoderDecoder> writerQueue = new LinkedBlockingQueue<>();
    private final IoFuture<StreamConnection> streamConnectionFuture;
    private volatile RedisClientConnection redisClientConnection;
    private volatile IOException failure;
    private volatile boolean closed = false;

    protected XnioRedisClient(IoFuture<StreamConnection> streamConnectionFuture, Pool<ByteBuffer> bufferPool,
            Charset charset) {
        this.streamConnectionFuture = streamConnectionFuture;
        this.streamConnectionFuture.addNotifier(new IoFuture.HandlingNotifier<StreamConnection, Void>() {
            @Override
            public void handleFailed(IOException exception, Void v) {
                failure = exception;
                CommandEncoderDecoder commandEncoderDecoder;
                while ((commandEncoderDecoder = writerQueue.poll()) != null) {
                    commandEncoderDecoder.fail(failure);
                }
            }

            @Override
            public void handleDone(StreamConnection data, Void v) {
                redisClientConnection = new RedisClientConnection(data, bufferPool, charset, writerQueue);
                if (!writerQueue.isEmpty()) {
                    redisClientConnection.commandAdded();
                }
            }
        }, null);
    }

    public <T> F send_(final Request<T> request) {
        if (closed) {
            return createCancelledFuture();
        }
        if (failure != null) {
            return createFailedFuture(failure);
        }
        final SF future = createFuture();
        writerQueue.add(new CommandEncoderDecoder() {
            private ReplyParser<? extends T> parser = request.parser();

            @Override
            public CommandWriter writer() {
                return request.writer();
            }

            @Override
            public boolean parse(ByteBuffer buffer, CharsetDecoder charsetDecoder) throws IOException {
                return parser.parseReply(buffer, value -> {
                    complete(future, value);
                    return true;
                }, partial -> {
                    parser = partial;
                    return false;
                }, message -> {
                    completeExceptionally(future, new RedisException(message.toString()));
                    return true;
                }, charsetDecoder);
            }

            @Override
            public void fail(Throwable e) {
                completeExceptionally(future, e);
            }

            @Override
            public void cancel() {
                XnioRedisClient.this.cancel(future);
            }
        });
        if (redisClientConnection != null) {
            redisClientConnection.commandAdded();
        }
        return future;
    }

    protected abstract F createCancelledFuture();

    protected abstract F createFailedFuture(Throwable exception);

    protected abstract SF createFuture();

    protected abstract <T> void complete(SF future, T value);

    protected abstract void completeExceptionally(SF future, Throwable exception);

    protected abstract void cancel(SF future);

    @Override
    public void close() {
        closed = true;
        IoUtils.safeClose(streamConnectionFuture);
        if (redisClientConnection != null) {
            redisClientConnection.close();
        }
    }
}
