package xnioredis.commands;

import xnioredis.Command;

public interface Command2<T1, T2, R> {
    Command<R> apply(T1 arg1, T2 arg2);
}
