package xnioredis;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.StreamConnection;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.net.InetSocketAddress;


public class ClientFactory implements AutoCloseable {
    private final XnioWorker worker;

    public ClientFactory(int ioThreads) throws IOException {
        Xnio xnio = Xnio.getInstance();
        worker = xnio.createWorker(OptionMap.create(Options.WORKER_IO_THREADS, ioThreads));
    }

    public ListenableFuture<RedisClient> connect(InetSocketAddress address) {
        IoFuture<StreamConnection> streamConnectionIoFuture = worker.openStreamConnection(address, null, OptionMap.EMPTY);
        return Futures.transform(new IoFutureAdapter<>(streamConnectionIoFuture), (Function<StreamConnection, RedisClient>) XnioRedisClient::new);
    }

    @Override
    public void close() {
        worker.shutdown();
    }
}
