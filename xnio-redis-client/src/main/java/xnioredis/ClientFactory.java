package xnioredis;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.net.InetSocketAddress;


public class ClientFactory implements AutoCloseable {
    private final XnioWorker worker;

    public ClientFactory() throws IOException {
        Xnio xnio = Xnio.getInstance();
        worker = xnio.createWorker(OptionMap.EMPTY);
    }

    ListenableFuture<RedisClient> connect(InetSocketAddress address) {
        IoFuture<StreamConnection> streamConnectionIoFuture = worker.openStreamConnection(address, null, OptionMap.EMPTY);
        return Futures.transform(new IoFutureAdapter<>(streamConnectionIoFuture), (Function<StreamConnection, RedisClient>) XnioRedisClient::new);
    }

    @Override
    public void close() {
        worker.shutdown();
    }
}
