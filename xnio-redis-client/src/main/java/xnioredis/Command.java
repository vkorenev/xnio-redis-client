package xnioredis;

import org.xnio.Pool;
import org.xnio.channels.StreamSinkChannel;
import xnioredis.decoder.parser.ReplyParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;

public interface Command<T> {
    void writeCommand(StreamSinkChannel channel, CharsetEncoder charsetEncoder, Pool<ByteBuffer> bufferPool) throws IOException;

    ReplyParser<T> parser();
}
