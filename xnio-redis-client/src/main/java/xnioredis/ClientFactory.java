package xnioredis;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ClientFactory implements AutoCloseable {
    private final Pool<ByteBuffer> byteBufferPool =
            new ByteBufferSlicePool(BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR, 4096, 4096 * 256);
    private final Charset charset;
    private final XnioWorker worker;

    public ClientFactory(Charset charset, int ioThreads) throws IOException {
        Xnio xnio = Xnio.getInstance();
        this.charset = charset;
        worker = xnio.createWorker(OptionMap.create(Options.WORKER_IO_THREADS, ioThreads));
    }

    public ListenableFuture<RedisClient> connect(InetSocketAddress address) {
        IoFuture<StreamConnection> streamConnectionIoFuture =
                worker.openStreamConnection(address, null, OptionMap.EMPTY);
        return Futures.transform(new IoFutureAdapter<>(streamConnectionIoFuture),
                (Function<StreamConnection, RedisClient>) connection ->
                        new XnioRedisClient(connection, byteBufferPool, charset));
    }

    @Override
    public void close() {
        worker.shutdown();
    }
}
