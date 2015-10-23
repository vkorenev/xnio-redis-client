package xnioredis;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import com.pholser.junit.quickcheck.ForAll;
import org.junit.contrib.theories.DataPoints;
import org.junit.contrib.theories.FromDataPoints;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;
import xnioredis.encoder.Encoders;
import xnioredis.encoder.RespArrayElementsWriter;
import xnioredis.encoder.RespSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

@RunWith(Theories.class)
public class EncodingTest {
    @DataPoints
    public static final Charset[] CHARSETS = {UTF_8, UTF_16BE, UTF_16LE, US_ASCII};

    @DataPoints("bufferSize")
    public static final int[] BUFFER_SIZES = {4, 5, 6, 7, 0x1000};

    @Theory
    public void testStrArg(@ForAll String val, Charset charset, @FromDataPoints("bufferSize") int bufferSize) throws
            Exception {
        assumeTrue(charset.newEncoder().canEncode(val));
        RespArrayElementsWriter writer = Encoders.strArg().encode(val);
        assertEquals(writer.size(), 1);
        assertThat(serialize(charset, bufferSize, writer), equalTo(respBulkString(val.getBytes(charset))));
    }

    @Theory
    public void testLongArg(@ForAll long val, Charset charset, @FromDataPoints("bufferSize") int bufferSize) throws
            Exception {
        RespArrayElementsWriter writer = Encoders.longArg().encode(val);
        assertEquals(writer.size(), 1);
        assertThat(serialize(charset, bufferSize, writer), equalTo(respBulkString(Long.toString(val).getBytes
                (ISO_8859_1))));
    }

    @Theory
    public void testIntArg(@ForAll int val, Charset charset, @FromDataPoints("bufferSize") int bufferSize) throws
            Exception {
        RespArrayElementsWriter writer = Encoders.intArg().encode(val);
        assertEquals(writer.size(), 1);
        assertThat(serialize(charset, bufferSize, writer), equalTo(respBulkString(Integer.toString(val).getBytes
                (ISO_8859_1))));
    }

    @Theory
    public void testBytesArg(@ForAll byte[] val, Charset charset, @FromDataPoints("bufferSize") int bufferSize)
            throws Exception {
        RespArrayElementsWriter writer = Encoders.bytesArg().encode(val);
        assertEquals(writer.size(), 1);
        assertThat(serialize(charset, bufferSize, writer), equalTo(respBulkString(val)));
    }

    @Theory
    public void testLongArrayArg(@ForAll long[] val, Charset charset, @FromDataPoints("bufferSize") int bufferSize)
            throws Exception {
        RespArrayElementsWriter writer = Encoders.longArrayArg().encode(val);
        assertEquals(writer.size(), val.length);
        assertThat(serialize(charset, bufferSize, writer), equalTo(Bytes.concat(Arrays.stream(val).mapToObj
                (Long::toString).map(s -> s.getBytes(ISO_8859_1)).map(this::respBulkString).toArray(byte[][]::new))));
    }

    @Theory
    public void testIntArrayArg(@ForAll int[] val, Charset charset, @FromDataPoints("bufferSize") int bufferSize)
            throws Exception {
        RespArrayElementsWriter writer = Encoders.intArrayArg().encode(val);
        assertEquals(writer.size(), val.length);
        assertThat(serialize(charset, bufferSize, writer), equalTo(Bytes.concat(Arrays.stream(val).mapToObj
                (Integer::toString).map(s -> s.getBytes(ISO_8859_1)).map(this::respBulkString).toArray(byte[][]::new)
        )));
    }

    private byte[] respBulkString(byte[] val) {
        byte[] lenBytes = Integer.toString(val.length).getBytes(ISO_8859_1);
        ByteArrayDataOutput byteArrayOutputStream = ByteStreams.newDataOutput(val.length + lenBytes.length + 5);
        byteArrayOutputStream.write('$');
        byteArrayOutputStream.write(lenBytes);
        byteArrayOutputStream.write('\r');
        byteArrayOutputStream.write('\n');
        byteArrayOutputStream.write(val);
        byteArrayOutputStream.write('\r');
        byteArrayOutputStream.write('\n');
        return byteArrayOutputStream.toByteArray();
    }

    private byte[] serialize(Charset charset, int bufferSize, RespArrayElementsWriter writer) throws IOException {
        ByteArraySink byteArraySink = new ByteArraySink();
        RespSink respSink = new ByteBuffersRespSink(byteArraySink, charset.newEncoder());
        writer.writeTo(respSink);
        return byteArraySink.geBytes();
    }

    private static class ByteArraySink implements ByteSink {
        private final ByteArrayDataOutput out = ByteStreams.newDataOutput();

        @Override
        public void write(byte b) {
            out.write(b);
        }

        @Override
        public void write(CharSequence s, CharsetEncoder charsetEncoder) throws CharacterCodingException {
            ByteBuffer byteBuffer = charsetEncoder.encode(CharBuffer.wrap(s));
            out.write(byteBuffer.array(), 0, byteBuffer.remaining());
        }

        @Override
        public void write(byte[] src) {
            out.write(src);
        }

        @Override
        public void write(byte[] src, int offset, int length) {
            out.write(src, offset, length);
        }

        public byte[] geBytes() {
            return out.toByteArray();
        }
    }
}
