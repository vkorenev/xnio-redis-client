package xnioredis.encoder;

import java.io.IOException;

public interface Encoder<T> {
    void write(CommandBuilder builder, T t) throws IOException;
}
