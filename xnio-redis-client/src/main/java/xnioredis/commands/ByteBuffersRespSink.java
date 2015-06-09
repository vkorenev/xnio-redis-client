package xnioredis.commands;

import com.google.common.base.Utf8;
import xnioredis.encoder.RespSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

class ByteBuffersRespSink implements RespSink {
    public static final byte[][] NUM_BYTES =
            IntStream.range(10, 99).mapToObj(i -> Integer.toString(i).getBytes(US_ASCII)).toArray(byte[][]::new);
    private final Supplier<ByteBuffer> writeBufferSupplier;
    private final CharsetEncoder charsetEncoder;
    private ByteBuffer buffer;

    ByteBuffersRespSink(Supplier<ByteBuffer> writeBufferSupplier, CharsetEncoder charsetEncoder) {
        this.writeBufferSupplier = writeBufferSupplier;
        this.charsetEncoder = charsetEncoder;
        this.buffer = this.writeBufferSupplier.get();
    }

    @Override
    public void array(int size) throws IOException {
        write((byte) '*');
        writeInt(size);
        writeCRLF();
    }

    @Override
    public void bulkString(CharSequence s) throws IOException {
        write((byte) '$');
        if (s.length() == 0) {
            write((byte) '0');
            writeCRLF();
        } else if (UTF_8.equals(charsetEncoder.charset())) {
            writeInt(Utf8.encodedLength(s));
            writeCRLF();
            write(s);
        } else if (charsetEncoder.maxBytesPerChar() == 1.0) {
            writeInt(s.length());
            writeCRLF();
            write(s);
        } else {
            ByteBuffer byteBuffer = encode(s);
            int encodedLength = byteBuffer.position();
            writeInt(encodedLength);
            writeCRLF();
            write(byteBuffer.array(), 0, encodedLength);
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
        write((byte) '$');
        writeInt(length);
        writeCRLF();
        write(src, offset, length);
        writeCRLF();
    }

    @Override
    public void bulkString(int num) throws IOException {
        if (num >= 0 && num <= 9) {
            write((byte) '$');
            write((byte) '1');
            writeCRLF();
            write((byte) ('0' + num));
            writeCRLF();
        } else if (num >= 10 && num <= 99) {
            write((byte) '$');
            write((byte) '2');
            writeCRLF();
            write(NUM_BYTES[num - 10]);
            writeCRLF();
        } else {
            bulkString(Integer.toString(num));
        }
    }

    @Override
    public void bulkString(long num) throws IOException {
        if (num >= 0 && num <= 9) {
            write((byte) '$');
            write((byte) '1');
            writeCRLF();
            write((byte) ('0' + (int) num));
            writeCRLF();
        } else if (num >= 10 && num <= 99) {
            write((byte) '$');
            write((byte) '2');
            writeCRLF();
            write(NUM_BYTES[(int) num - 10]);
            writeCRLF();
        } else {
            bulkString(Long.toString(num));
        }
    }

    private void write(byte b) {
        if (!buffer.hasRemaining()) {
            buffer = writeBufferSupplier.get();
        }
        buffer.put(b);
    }

    private void writeInt(int num) throws CharacterCodingException {
        if (num >= 0 && num <= 9) {
            write((byte) ('0' + num));
        } else if (num >= 10 && num <= 99) {
            write(NUM_BYTES[num - 10]);
        } else {
            write(Integer.toString(num));
        }
    }

    private void writeCRLF() {
        write((byte) '\r');
        write((byte) '\n');
    }

    private void write(CharSequence s) throws CharacterCodingException {
        CharBuffer in = CharBuffer.wrap(s);
        try {
            while (true) {
                CoderResult coderResult =
                        in.hasRemaining() ? charsetEncoder.encode(in, buffer, true) : CoderResult.UNDERFLOW;
                if (coderResult.isUnderflow()) {
                    coderResult = charsetEncoder.flush(buffer);
                }
                if (coderResult.isUnderflow()) {
                    break;
                } else if (coderResult.isOverflow()) {
                    buffer = writeBufferSupplier.get();
                } else {
                    coderResult.throwException();
                }
            }
        } finally {
            charsetEncoder.reset();
        }
    }

    private void write(byte[] src) {
        write(src, 0, src.length);
    }

    private void write(byte[] src, int offset, int length) {
        while (true) {
            int freeSpace = buffer.remaining();
            if (freeSpace >= length) {
                buffer.put(src, offset, length);
                break;
            } else {
                buffer.put(src, offset, freeSpace);
                buffer = writeBufferSupplier.get();
                offset += freeSpace;
                length -= freeSpace;
            }
        }
    }
}
