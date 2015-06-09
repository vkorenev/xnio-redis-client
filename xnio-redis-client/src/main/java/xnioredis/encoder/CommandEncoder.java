package xnioredis.encoder;

import java.io.IOException;

public interface CommandEncoder {
    void encode(RespSink sink) throws IOException;
}
