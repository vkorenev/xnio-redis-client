package xnioredis.encoder;

import java.io.IOException;

public interface CommandBuilder {
    void array(int size) throws IOException;

    void bulkString(CharSequence s) throws IOException;

    void bulkString(byte[] src, int offset, int length) throws IOException;

    void bulkString(byte[] src) throws IOException;
}
