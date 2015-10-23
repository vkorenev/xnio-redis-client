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
import java.util.stream.LongStream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

class ByteBuffersRespSink implements RespSink {
    public static final byte[][] NUM_BYTES =
            IntStream.rangeClosed(10, 99).mapToObj(i -> Integer.toString(i).getBytes(US_ASCII)).toArray(byte[][]::new);
    private static final byte[] MIN_LONG_BYTES = "-9223372036854775808".getBytes(US_ASCII);
    private static final long[] SIZE_TABLE = LongStream.iterate(10, x -> x * 10).limit(18).map(x -> x - 1).toArray();
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
            oneDigitAsBulkString(num);
        } else if (num >= 10 && num <= 99) {
            twoDigitsAsBulkString(NUM_BYTES[num - 10]);
        } else {
            longAsBulkString(num);
        }
    }

    @Override
    public void bulkString(long num) throws IOException {
        if (num >= 0 && num <= 9) {
            oneDigitAsBulkString((int) num);
        } else if (num >= 10 && num <= 99) {
            twoDigitsAsBulkString(NUM_BYTES[(int) num - 10]);
        } else if (num == Long.MIN_VALUE) {
            minLongAsBulkString();
        } else {
            longAsBulkString(num);
        }
    }

    private void oneDigitAsBulkString(int num) {
        byteSink.write((byte) '$');
        byteSink.write((byte) '1');
        writeCRLF();
        byteSink.write((byte) ('0' + num));
        writeCRLF();
    }

    private void twoDigitsAsBulkString(byte[] numByte) {
        byteSink.write((byte) '$');
        byteSink.write((byte) '2');
        writeCRLF();
        byteSink.write(numByte);
        writeCRLF();
    }

    private void minLongAsBulkString() {
        byteSink.write((byte) '$');
        byteSink.write((byte) '2');
        byteSink.write((byte) '0');
        writeCRLF();
        byteSink.write(MIN_LONG_BYTES);
        writeCRLF();
    }

    private void longAsBulkString(long num) {
        byteSink.write((byte) '$');
        byte[] bytes = toBytes(num);
        int len = bytes.length;
        if (len <= 9) {
            byteSink.write((byte) ('0' + len));
        } else {
            byteSink.write(NUM_BYTES[len - 10]);
        }
        writeCRLF();
        byteSink.write(bytes);
        writeCRLF();
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
            byteSink.write(toBytes(num));
        }
    }

    static byte[] toBytes(long num) {
        if (num == Long.MIN_VALUE) return MIN_LONG_BYTES;
        boolean neg = num < 0;
        if (neg) {
            num = -num;
        }
        int size = neg ? stringSize(num) + 1 : stringSize(num);
        byte[] buf = new byte[size];
        if (neg) {
            buf[0] = '-';
        }
        int i = size - 1;
        while (num != 0) {
            buf[i--] = (byte) ('0' + num % 10);
            num /= 10;
        }
        return buf;
    }

    private static int stringSize(long x) {
        for (int i = 0; i < SIZE_TABLE.length; i++) {
            if (x <= SIZE_TABLE[i]) {
                return i + 1;
            }
        }
        return 19;
    }

    private void writeCRLF() {
        byteSink.write((byte) '\r');
        byteSink.write((byte) '\n');
    }
}
