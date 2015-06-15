package xnioredis;

import xnioredis.encoder.RespSink;

import java.io.IOException;

public interface CommandWriter {
    void write(RespSink sink) throws IOException;
}
