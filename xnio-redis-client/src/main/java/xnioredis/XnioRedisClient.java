package xnioredis;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
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
    private final RedisClientConnection clientConnection;

    public XnioRedisClient(StreamConnection connection, Pool<ByteBuffer> bufferPool, Charset charset) {
        clientConnection = new RedisClientConnection(connection, bufferPool, charset, writerQueue);
    }

    @Override
    public <T> ListenableFuture<T> send(Command<T> command) {
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
        });
        clientConnection.commandAdded();
        return future;
    }

    @Override
    public void close() {
        clientConnection.close();
    }
}
