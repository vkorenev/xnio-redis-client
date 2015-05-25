package xnioredis;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import xnioredis.commands.Command1;
import xnioredis.commands.Command2;
import xnioredis.commands.Command3;

public abstract class RedisClient implements AutoCloseable {
    public final <R> ListenableFuture<R> send(Command<R> command) {
        return send(command, true);
    }

    public final <T, R> ListenableFuture<R> send(Command1<T, R> command, T arg) {
        return send(command.apply(arg));
    }

    public final <T1, T2, R> ListenableFuture<R> send(Command2<T1, T2, R> command, T1 arg1, T2 arg2) {
        return send(command.apply(arg1, arg2));
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

    public final <R> ListenableFuture<R> sendNoFlush(Command<R> command) {
        return send(command, false);
    }

    public final <T, R> ListenableFuture<R> sendNoFlush(Command1<T, R> command, T arg) {
        return sendNoFlush(command.apply(arg));
    }

    public final <T1, T2, R> ListenableFuture<R> sendNoFlush(Command2<T1, T2, R> command, T1 arg1, T2 arg2) {
        return sendNoFlush(command.apply(arg1, arg2));
    }

    public final <T1, T2, T3, R> ListenableFuture<R> sendNoFlush(Command3<T1, T2, T3, R> command, T1 arg1, T2 arg2, T3 arg3) {
        return sendNoFlush(command.apply(arg1, arg2, arg3));
    }

    @SafeVarargs
    public final <E, R> ListenableFuture<R> sendNoFlush(Command1<E[], R> command, E... arg1) {
        return sendNoFlush(command.apply(arg1));
    }

    @SafeVarargs
    public final <T, E, R> ListenableFuture<R> sendNoFlush(Command2<T, E[], R> command, T arg1, E... arg2) {
        return sendNoFlush(command.apply(arg1, arg2));
    }

    public abstract <R> ListenableFuture<R> send(Command<R> command, boolean autoFlush);

    public static <R> ListenableFuture<R> send(
            ListenableFuture<RedisClient> clientFuture, Command<R> command) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.send(command));
    }

    public static <T, R> ListenableFuture<R> send(
            ListenableFuture<RedisClient> clientFuture, Command1<T, R> command, T arg) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.send(command, arg));
    }

    public static <T1, T2, R> ListenableFuture<R> send(
            ListenableFuture<RedisClient> clientFuture, Command2<T1, T2, R> command, T1 arg1, T2 arg2) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.send(command, arg1, arg2));
    }

    public static <T1, T2, T3, R> ListenableFuture<R> send(
            ListenableFuture<RedisClient> clientFuture, Command3<T1, T2, T3, R> command, T1 arg1, T2 arg2, T3 arg3) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.send(command, arg1, arg2, arg3));
    }

    @SafeVarargs
    public static <E, R> ListenableFuture<R> send(
            ListenableFuture<RedisClient> clientFuture, Command1<E[], R> command, E... arg1) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.send(command, arg1));
    }

    @SafeVarargs
    public static <T, E, R> ListenableFuture<R> send(
            ListenableFuture<RedisClient> clientFuture, Command2<T, E[], R> command, T arg1, E... arg2) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.send(command, arg1, arg2));
    }

    public static <R> ListenableFuture<R> sendNoFlush(
            ListenableFuture<RedisClient> clientFuture, Command<R> command) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.sendNoFlush(command));
    }

    public static <T, R> ListenableFuture<R> sendNoFlush(
            ListenableFuture<RedisClient> clientFuture, Command1<T, R> command, T arg) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.sendNoFlush(command, arg));
    }

    public static <T1, T2, R> ListenableFuture<R> sendNoFlush(
            ListenableFuture<RedisClient> clientFuture, Command2<T1, T2, R> command, T1 arg1, T2 arg2) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.sendNoFlush(command, arg1, arg2));
    }

    public static <T1, T2, T3, R> ListenableFuture<R> sendNoFlush(
            ListenableFuture<RedisClient> clientFuture, Command3<T1, T2, T3, R> command, T1 arg1, T2 arg2, T3 arg3) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.sendNoFlush(command, arg1, arg2, arg3));
    }

    @SafeVarargs
    public static <E, R> ListenableFuture<R> sendNoFlush(
            ListenableFuture<RedisClient> clientFuture, Command1<E[], R> command, E... arg1) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.sendNoFlush(command, arg1));
    }

    @SafeVarargs
    public static <T, E, R> ListenableFuture<R> sendNoFlush(
            ListenableFuture<RedisClient> clientFuture, Command2<T, E[], R> command, T arg1, E... arg2) {
        return Futures.transform(clientFuture, (AsyncFunction<RedisClient, R>) client ->
                client.sendNoFlush(command, arg1, arg2));
    }

    public abstract ListenableFuture<Void> flush();

    @Override
    public abstract void close();
}
