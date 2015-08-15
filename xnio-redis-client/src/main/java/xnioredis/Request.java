package xnioredis;

import xnioredis.decoder.parser.ReplyParser;

public interface Request<T> {
    CommandWriter writer();

    ReplyParser<? extends T> parser();
}
