package xnioredis.encoder;

import java.io.IOException;

public interface RespArrayElementsWriter {
    default int size() {
        return 1;
    }

    void writeTo(RespSink sink) throws IOException;
}
