package xnioredis;

import java.io.IOException;
import java.nio.ByteBuffer;

interface ReplyDecoder {
    void parse(ByteBuffer buffer) throws IOException;

    void fail(Exception e);
}
