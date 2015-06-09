package xnioredis.encoder;

import java.io.IOException;

public interface Encoder<T> {
    void write(RespSink sink, T t) throws IOException;
}
