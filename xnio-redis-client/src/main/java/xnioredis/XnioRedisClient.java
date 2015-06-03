package xnioredis;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import xnioredis.RedisClientConnection.CommandEncoderDecoder;
import xnioredis.decoder.parser.ReplyParser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class XnioRedisClient extends RedisClient {
    private final BlockingQueue<CommandEncoderDecoder> writerQueue = new LinkedBlockingQueue<>();
    private final IoFuture<StreamConnection> streamConnectionFuture;
    private volatile RedisClientConnection redisClientConnection;
    private volatile IOException failure;
    private volatile boolean closed = false;

    XnioRedisClient(IoFuture<StreamConnection> streamConnectionFuture, Pool<ByteBuffer> bufferPool, Charset charset) {
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

    @Override
    public <T> ListenableFuture<T> send(Command<T> command) {
        if (closed) {
            return Futures.immediateCancelledFuture();
        }
        if (failure != null) {
            return Futures.immediateFailedFuture(failure);
        }
        SettableFuture<T> future = SettableFuture.create();
        writerQueue.add(new CommandEncoderDecoder() {
            private ReplyParser<? extends T> parser = command.parser();

            @Override
            public CommandWriter writer() {
                return command.writer();
            }

            @Override
            public boolean parse(ByteBuffer buffer, CharsetDecoder charsetDecoder) throws IOException {
                return parser.parseReply(buffer, new ReplyParser.ReplyVisitor<T, Boolean>() {
                    @Override
                    public Boolean success(@Nullable T value) {
                        future.set(value);
                        return true;
                    }

                    @Override
                    public Boolean failure(CharSequence message) {
                        future.setException(new RedisException(message.toString()));
                        return true;
                    }

                    @Override
                    public Boolean partialReply(ReplyParser<? extends T> partial) {
                        parser = partial;
                        return false;
                    }
                }, charsetDecoder);
            }

            @Override
            public void fail(Throwable e) {
                future.setException(e);
            }

            @Override
            public void cancel() {
                future.cancel(true);
            }
        });
        if (redisClientConnection != null) {
            redisClientConnection.commandAdded();
        }
        return future;
    }

    @Override
    public void close() {
        closed = true;
        IoUtils.safeClose(streamConnectionFuture);
        if (redisClientConnection != null) {
            redisClientConnection.close();
        }
    }
}
