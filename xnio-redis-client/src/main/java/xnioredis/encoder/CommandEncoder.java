package xnioredis.encoder;

import java.io.IOException;

public interface CommandEncoder {
    void encode(CommandBuilder commandBuilder) throws IOException;
}
