package xnioredis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.function.Supplier;

public interface CommandWriter {
    void write(Supplier<ByteBuffer> writeBufferSupplier, CharsetEncoder charsetEncoder) throws IOException;
}
