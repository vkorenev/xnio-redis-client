package xnioredis;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;

interface ByteSink {
    void write(byte b);

    void write(CharSequence s, CharsetEncoder charsetEncoder) throws CharacterCodingException;

    void write(byte[] src);

    void write(byte[] src, int offset, int length);
}
