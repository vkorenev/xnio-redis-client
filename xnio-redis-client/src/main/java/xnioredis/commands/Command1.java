package xnioredis.commands;

import xnioredis.Command;

public interface Command1<T, R> {
    Command<R> apply(T arg);
}
