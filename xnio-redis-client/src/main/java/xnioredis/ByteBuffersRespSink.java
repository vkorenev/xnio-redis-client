package xnioredis;

import com.google.common.base.Utf8;
import xnioredis.encoder.RespSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

class ByteBuffersRespSink implements RespSink {
    public static final byte[][] NUM_BYTES =
            IntStream.range(10, 99).mapToObj(i -> Integer.toString(i).getBytes(US_ASCII)).toArray(byte[][]::new);
    private final CharsetEncoder charsetEncoder;
    private final ByteSink byteSink;

    ByteBuffersRespSink(ByteSink byteSink, CharsetEncoder charsetEncoder) {
        this.byteSink = byteSink;
        this.charsetEncoder = charsetEncoder;
    }

    @Override
    public void array(int size) throws IOException {
        byteSink.write((byte) '*');
        writeInt(size);
        writeCRLF();
    }

    @Override
    public void bulkString(CharSequence s) throws IOException {
        byteSink.write((byte) '$');
        if (s.length() == 0) {
            byteSink.write((byte) '0');
            writeCRLF();
        } else if (UTF_8.equals(charsetEncoder.charset())) {
            writeInt(Utf8.encodedLength(s));
            writeCRLF();
            byteSink.write(s, charsetEncoder);
        } else if (charsetEncoder.maxBytesPerChar() == 1.0) {
            writeInt(s.length());
            writeCRLF();
            byteSink.write(s, charsetEncoder);
        } else {
            ByteBuffer byteBuffer = encode(s);
            int encodedLength = byteBuffer.position();
            writeInt(encodedLength);
            writeCRLF();
            byteSink.write(byteBuffer.array(), 0, encodedLength);
        }
        writeCRLF();
    }

    private ByteBuffer encode(CharSequence s) throws CharacterCodingException {
        int maxLength = (int) (s.length() * (double) charsetEncoder.maxBytesPerChar());
        ByteBuffer byteBuffer = ByteBuffer.allocate(maxLength);
        CharBuffer charBuffer = CharBuffer.wrap(s);
        try {
            CoderResult coderResult = charsetEncoder.encode(charBuffer, byteBuffer, true);
            if (coderResult.isUnderflow()) {
                coderResult = charsetEncoder.flush(byteBuffer);
            }
            if (!coderResult.isUnderflow()) {
                coderResult.throwException();
            }
        } finally {
            charsetEncoder.reset();
        }
        return byteBuffer;
    }

    @Override
    public void bulkString(byte[] src) throws IOException {
        bulkString(src, 0, src.length);
    }

    @Override
    public void bulkString(byte[] src, int offset, int length) throws IOException {
        byteSink.write((byte) '$');
        writeInt(length);
        writeCRLF();
        byteSink.write(src, offset, length);
        writeCRLF();
    }

    @Override
    public void bulkString(int num) throws IOException {
        if (num >= 0 && num <= 9) {
            byteSink.write((byte) '$');
            byteSink.write((byte) '1');
            writeCRLF();
            byteSink.write((byte) ('0' + num));
            writeCRLF();
        } else if (num >= 10 && num <= 99) {
            byteSink.write((byte) '$');
            byteSink.write((byte) '2');
            writeCRLF();
            byteSink.write(NUM_BYTES[num - 10]);
            writeCRLF();
        } else {
            bulkString(Integer.toString(num));
        }
    }

    @Override
    public void bulkString(long num) throws IOException {
        if (num >= 0 && num <= 9) {
            byteSink.write((byte) '$');
            byteSink.write((byte) '1');
            writeCRLF();
            byteSink.write((byte) ('0' + (int) num));
            writeCRLF();
        } else if (num >= 10 && num <= 99) {
            byteSink.write((byte) '$');
            byteSink.write((byte) '2');
            writeCRLF();
            byteSink.write(NUM_BYTES[(int) num - 10]);
            writeCRLF();
        } else {
            bulkString(Long.toString(num));
        }
    }

    @Override
    public void writeRaw(byte[] bytes) throws IOException {
        byteSink.write(bytes);
    }

    private void writeInt(int num) throws CharacterCodingException {
        if (num >= 0 && num <= 9) {
            byteSink.write((byte) ('0' + num));
        } else if (num >= 10 && num <= 99) {
            byteSink.write(NUM_BYTES[num - 10]);
        } else {
            byteSink.write(Integer.toString(num), charsetEncoder);
        }
    }

    private void writeCRLF() {
        byteSink.write((byte) '\r');
        byteSink.write((byte) '\n');
    }
}
