package xnioredis.commands;

import com.google.common.base.Utf8;
import org.xnio.Buffers;
import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.channels.StreamSinkChannel;
import xnioredis.Command;
import xnioredis.CommandWriter;
import xnioredis.decoder.parser.ReplyParser;
import xnioredis.encoder.CommandBuilder;
import xnioredis.encoder.CommandEncoder;
import xnioredis.encoder.Encoder;
import xnioredis.encoder.MultiEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class Commands {
    private static final byte[] DEL = bytes("DEL");
    private static final byte[] ECHO = bytes("ECHO");
    private static final byte[] FLUSHALL = bytes("FLUSHALL");
    private static final byte[] FLUSHDB = bytes("FLUSHDB");
    private static final byte[] GET = bytes("GET");
    private static final byte[] HDEL = bytes("HDEL");
    private static final byte[] HGET = bytes("HGET");
    private static final byte[] HGETALL = bytes("HGETALL");
    private static final byte[] HINCRBY = bytes("HINCRBY");
    private static final byte[] HKEYS = bytes("HKEYS");
    private static final byte[] HLEN = bytes("HLEN");
    private static final byte[] HMGET = bytes("HMGET");
    private static final byte[] HMSET = bytes("HMSET");
    private static final byte[] HSET = bytes("HSET");
    private static final byte[] SADD = bytes("SADD");
    private static final byte[] SET = bytes("SET");
    private static final byte[] SETNX = bytes("SETNX");
    private static final byte[] SMEMBERS = bytes("SMEMBERS");
    private static final byte[] PING = bytes("PING");

    public static <K, R> Command1<K, R> del(
            MultiEncoder<? super K> keysEncoder, ReplyParser<? extends R> replyParser) {
        return (keys) -> define(cb -> {
            cb.array(1 + keysEncoder.size(keys));
            cb.bulkString(DEL);
            keysEncoder.write(cb, keys);
        }, replyParser);
    }

    public static <K, R> Command1<K, R> echo(
            Encoder<? super K> messageEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(cb -> {
            cb.array(2);
            cb.bulkString(ECHO);
            messageEncoder.write(cb, key);
        }, replyParser);
    }

    public static <R> Command<R> flushall(ReplyParser<? extends R> replyParser) {
        return define(cb -> {
            cb.array(1);
            cb.bulkString(FLUSHALL);
        }, replyParser);
    }

    public static <R> Command<R> flushdb(ReplyParser<? extends R> replyParser) {
        return define(cb -> {
            cb.array(1);
            cb.bulkString(FLUSHDB);
        }, replyParser);
    }

    public static <K, R> Command1<K, R> get(
            Encoder<? super K> keyEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(cb -> {
            cb.array(2);
            cb.bulkString(GET);
            keyEncoder.write(cb, key);
        }, replyParser);
    }

    public static <K, F, R> Command2<K, F, R> hdel(
            Encoder<? super K> keyEncoder, MultiEncoder<? super F> fieldsEncoder, ReplyParser<? extends R> replyParser) {
        return (key, fields) -> define(cb -> {
            cb.array(2 + fieldsEncoder.size(fields));
            cb.bulkString(HDEL);
            keyEncoder.write(cb, key);
            fieldsEncoder.write(cb, fields);
        }, replyParser);
    }

    public static <K, F, R> Command2<K, F, R> hget(
            Encoder<? super K> keyEncoder, Encoder<? super F> fieldEncoder, ReplyParser<? extends R> replyParser) {
        return (key, field) -> define(cb -> {
            cb.array(3);
            cb.bulkString(HGET);
            keyEncoder.write(cb, key);
            fieldEncoder.write(cb, field);
        }, replyParser);
    }

    public static <K, R> Command1<K, R> hgetall(
            Encoder<? super K> keyEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(cb -> {
            cb.array(2);
            cb.bulkString(HGETALL);
            keyEncoder.write(cb, key);
        }, replyParser);
    }

    public static <K, R> Command1<K, R> hkeys(
            Encoder<? super K> keyEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(cb -> {
            cb.array(2);
            cb.bulkString(HKEYS);
            keyEncoder.write(cb, key);
        }, replyParser);
    }

    public static <K, F, V, R> Command3<K, F, V, R> hincrby(
            Encoder<? super K> keyEncoder, Encoder<? super F> fieldEncoder, Encoder<? super V> incrementEncoder, ReplyParser<? extends R> replyParser) {
        return (key, field, increment) -> define(cb -> {
            cb.array(4);
            cb.bulkString(HINCRBY);
            keyEncoder.write(cb, key);
            fieldEncoder.write(cb, field);
            incrementEncoder.write(cb, increment);
        }, replyParser);
    }

    public static <K, R> Command1<K, R> hlen(
            Encoder<? super K> keyEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(cb -> {
            cb.array(2);
            cb.bulkString(HLEN);
            keyEncoder.write(cb, key);
        }, replyParser);
    }

    public static <K, F, R> Command2<K, F, R> hmget(
            Encoder<? super K> keyEncoder, MultiEncoder<? super F> fieldsEncoder, ReplyParser<? extends R> replyParser) {
        return (key, fields) -> define(cb -> {
            cb.array(2 + fieldsEncoder.size(fields));
            cb.bulkString(HMGET);
            keyEncoder.write(cb, key);
            fieldsEncoder.write(cb, fields);
        }, replyParser);
    }

    public static <K, F, R> Command2<K, F, R> hmset(
            Encoder<? super K> keyEncoder, MultiEncoder<? super F> fieldsEncoder, ReplyParser<? extends R> replyParser) {
        return (key, fields) -> define(cb -> {
            cb.array(2 + fieldsEncoder.size(fields));
            cb.bulkString(HMSET);
            keyEncoder.write(cb, key);
            fieldsEncoder.write(cb, fields);
        }, replyParser);
    }

    public static <K, F, V, R> Command3<K, F, V, R> hset(
            Encoder<? super K> keyEncoder, Encoder<? super F> fieldEncoder, Encoder<? super V> valueEncoder, ReplyParser<? extends R> replyParser) {
        return (key, field, value) -> define(cb -> {
            cb.array(4);
            cb.bulkString(HSET);
            keyEncoder.write(cb, key);
            fieldEncoder.write(cb, field);
            valueEncoder.write(cb, value);
        }, replyParser);
    }

    public static <K, V, R> Command2<K, V, R> sadd(
            Encoder<? super K> keyEncoder, MultiEncoder<? super V> valuesEncoder, ReplyParser<? extends R> replyParser) {
        return (key, values) -> define(cb -> {
            cb.array(2 + valuesEncoder.size(values));
            cb.bulkString(SADD);
            keyEncoder.write(cb, key);
            valuesEncoder.write(cb, values);
        }, replyParser);
    }

    public static <K, V, R> Command2<K, V, R> set(
            Encoder<? super K> keyEncoder, Encoder<? super V> valueEncoder, ReplyParser<? extends R> replyParser) {
        return (key, value) -> define(cb -> {
            cb.array(3);
            cb.bulkString(SET);
            keyEncoder.write(cb, key);
            valueEncoder.write(cb, value);
        }, replyParser);
    }

    public static <K, V, R> Command2<K, V, R> setnx(
            Encoder<? super K> keyEncoder, Encoder<? super V> valueEncoder, ReplyParser<? extends R> replyParser) {
        return (key, value) -> define(cb -> {
            cb.array(3);
            cb.bulkString(SETNX);
            keyEncoder.write(cb, key);
            valueEncoder.write(cb, value);
        }, replyParser);
    }

    public static <K, R> Command1<K, R> smembers(
            Encoder<? super K> keyEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(cb -> {
            cb.array(2);
            cb.bulkString(SMEMBERS);
            keyEncoder.write(cb, key);
        }, replyParser);
    }

    public static <R> Command<R> ping(ReplyParser<? extends R> replyParser) {
        return define(cb -> {
            cb.array(1);
            cb.bulkString(PING);
        }, replyParser);
    }

    private static byte[] bytes(String commandName) {
        return commandName.getBytes(US_ASCII);
    }

    private static <T> Command<T> define(CommandEncoder encoder, ReplyParser<? extends T> parser) {
        return new Command<T>() {
            @Override
            public CommandWriter writer() {
                return new CommandWriter() {
                    private final List<Pooled<ByteBuffer>> pooledBuffers = new ArrayList<>();
                    private boolean firstTime = true;

                    @Override
                    public boolean write(StreamSinkChannel channel, CharsetEncoder charsetEncoder, Pool<ByteBuffer> bufferPool) throws IOException {
                        if (firstTime) {
                            CommandBuilderImpl commandBuilder = new CommandBuilderImpl(() -> {
                                Pooled<ByteBuffer> pooledBuffer = bufferPool.allocate();
                                pooledBuffers.add(pooledBuffer);
                                return pooledBuffer.getResource();
                            }, charsetEncoder);
                            encoder.encode(commandBuilder);
                            commandBuilder.finish();
                            firstTime = false;
                        }
                        ByteBuffer[] byteBuffers = pooledBuffers.stream().map(Pooled::getResource).toArray(ByteBuffer[]::new);
                        while (Buffers.hasRemaining(byteBuffers, 0, byteBuffers.length)) {
                            long res = channel.write(byteBuffers, 0, byteBuffers.length);
                            if (res == 0) {
                                return false;
                            }
                        }
                        pooledBuffers.forEach(Pooled::free);
                        pooledBuffers.clear();
                        return true;
                    }
                };
            }

            @Override
            public ReplyParser<? extends T> parser() {
                return parser;
            }
        };
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
