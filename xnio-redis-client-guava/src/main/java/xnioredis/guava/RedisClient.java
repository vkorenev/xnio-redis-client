package xnioredis.guava;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.xnio.IoFuture;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import xnioredis.Command;
import xnioredis.Request;
import xnioredis.XnioRedisClient;
import xnioredis.commands.Command1;
import xnioredis.commands.Command2;
import xnioredis.commands.Command3;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class RedisClient extends XnioRedisClient<ListenableFuture, SettableFuture> {
    RedisClient(IoFuture<StreamConnection> streamConnectionFuture, Pool<ByteBuffer> bufferPool, Charset charset) {
        super(streamConnectionFuture, bufferPool, charset);
    }

    @Override
    protected ListenableFuture createCancelledFuture() {
        return Futures.immediateCancelledFuture();
    }

    @Override
    protected ListenableFuture createFailedFuture(Throwable exception) {
        return Futures.immediateFailedFuture(exception);
    }

    @Override
    protected SettableFuture createFuture() {
        return SettableFuture.create();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> void complete(SettableFuture future, T value) {
        future.set(value);
    }

    @Override
    protected void completeExceptionally(SettableFuture future, Throwable exception) {
        future.setException(exception);
    }

    @Override
    protected void cancel(SettableFuture future) {
        future.cancel(true);
    }

    @SuppressWarnings("unchecked")
    public <T> ListenableFuture<T> send(Request<T> request) {
        return send_(request);
    }

    public final <V, R> ListenableFuture<R> send(Command<R> command, Command.OptionalValue<V> opt, V val) {
        return send(command.append(opt, val));
    }

    public final <T, R> ListenableFuture<R> send(Command1<T, R> command, T arg) {
        return send(command.apply(arg));
    }

    public final <T, V, R> ListenableFuture<R> send(Command1<T, R> command, T arg, Command.OptionalValue<V> opt,
            V val) {
        return send(command.apply(arg).append(opt, val));
    }

    public final <T1, T2, R> ListenableFuture<R> send(Command2<T1, T2, R> command, T1 arg1, T2 arg2) {
        return send(command.apply(arg1, arg2));
    }

    public final <T1, T2, V, R> ListenableFuture<R> send(Command2<T1, T2, R> command, T1 arg1, T2 arg2,
            Command.OptionalValue<V> opt, V val) {
        return send(command.apply(arg1, arg2).append(opt, val));
    }

    public final <T1, T2, T3, R> ListenableFuture<R> send(Command3<T1, T2, T3, R> command, T1 arg1, T2 arg2, T3 arg3) {
        return send(command.apply(arg1, arg2, arg3));
    }

    @SafeVarargs
    public final <E, R> ListenableFuture<R> send(Command1<E[], R> command, E... arg1) {
        return send(command.apply(arg1));
    }

    @SafeVarargs
    public final <T, E, R> ListenableFuture<R> send(Command2<T, E[], R> command, T arg1, E... arg2) {
        return send(command.apply(arg1, arg2));
    }
}
