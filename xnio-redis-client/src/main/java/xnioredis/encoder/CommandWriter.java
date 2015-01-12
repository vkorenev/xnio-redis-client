package xnioredis.encoder;

import com.google.common.base.Utf8;
import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.channels.Channels;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CommandWriter {
    public static void writeCommand(StreamSinkChannel channel, CommandEncoder encoder, CharsetEncoder charsetEncoder, Pool<ByteBuffer> bufferPool) throws IOException {
        List<Pooled<ByteBuffer>> pooledBuffers = new ArrayList<>();
        try {
            CommandBuilderImpl commandBuilder = new CommandBuilderImpl(() -> {
                Pooled<ByteBuffer> pooledBuffer = bufferPool.allocate();
                pooledBuffers.add(pooledBuffer);
                return pooledBuffer.getResource();
            }, charsetEncoder);
            encoder.encode(commandBuilder);
            commandBuilder.finish();
            ByteBuffer[] byteBuffers = pooledBuffers.stream().map(Pooled::getResource).toArray(ByteBuffer[]::new);
            Channels.writeBlocking(channel, byteBuffers, 0, byteBuffers.length);
        } finally {
            pooledBuffers.forEach(Pooled::free);
        }
    }

    private static class CommandBuilderImpl implements CommandBuilder {
        private final Supplier<ByteBuffer> bufferSupplier;
        private final CharsetEncoder charsetEncoder;
        private ByteBuffer buffer;

        public CommandBuilderImpl(Supplier<ByteBuffer> bufferSupplier, CharsetEncoder charsetEncoder) {
            this.bufferSupplier = bufferSupplier;
            this.charsetEncoder = charsetEncoder;
            this.buffer = this.bufferSupplier.get();
        }

        @Override
        public void array(int size) throws IOException {
            write((byte) '*');
            write(Integer.toString(size));
            writeCRLF();
        }

        @Override
        public void bulkString(CharSequence s) throws IOException {
            write((byte) '$');
            write(Integer.toString(Utf8.encodedLength(s)));
            writeCRLF();
            write(s);
            writeCRLF();
        }

        @Override
        public void bulkString(byte[] src, int offset, int length) throws IOException {
            write((byte) '$');
            write(Integer.toString(length));
            writeCRLF();
            write(src, offset, length);
            writeCRLF();
        }

        @Override
        public void bulkString(byte[] src) throws IOException {
            bulkString(src, 0, src.length);
        }

        private void finish() {
            buffer.flip();
        }

        private void write(byte b) {
            if (!buffer.hasRemaining()) {
                buffer.flip();
                buffer = bufferSupplier.get();
            }
            buffer.put(b);
        }

        private void writeCRLF() {
            write((byte) '\r');
            write((byte) '\n');
        }

        private void write(CharSequence s) throws CharacterCodingException {
            CharBuffer in = CharBuffer.wrap(s);
            while (true) {
                CoderResult coderResult = in.hasRemaining() ? charsetEncoder.encode(in, buffer, true) : CoderResult.UNDERFLOW;
                if (coderResult.isUnderflow()) {
                    coderResult = charsetEncoder.flush(buffer);
                }
                if (coderResult.isUnderflow()) {
                    break;
                } else if (coderResult.isOverflow()) {
                    buffer.flip();
                    buffer = bufferSupplier.get();
                } else {
                    coderResult.throwException();
                }
            }
            charsetEncoder.reset();
        }

        private void write(byte[] src, int offset, int length) {
            while (true) {
                int freeSpace = buffer.remaining();
                if (freeSpace >= length) {
                    buffer.put(src, offset, length);
                    break;
                } else {
                    buffer.put(src, offset, freeSpace);
                    buffer.flip();
                    buffer = bufferSupplier.get();
                    offset += freeSpace;
                    length -= freeSpace;
                }
            }
        }
    }
}
