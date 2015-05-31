package xnioredis;

import xnioredis.decoder.parser.ReplyParser;

public interface Command<T> {
    CommandWriter writer();

    ReplyParser<? extends T> parser();
}
