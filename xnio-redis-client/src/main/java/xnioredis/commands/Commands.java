package xnioredis.commands;

import xnioredis.Command;
import xnioredis.CommandWriter;
import xnioredis.decoder.parser.ReplyParser;
import xnioredis.encoder.Encoder;
import xnioredis.encoder.MultiEncoder;
import xnioredis.encoder.RespArrayElementsWriter;
import xnioredis.encoder.RespSink;

public class Commands {
    private static final RespArrayElementsWriter DEL = new BulkStringLiteral("DEL");
    private static final RespArrayElementsWriter ECHO = new BulkStringLiteral("ECHO");
    private static final RespArrayElementsWriter FLUSHALL = new BulkStringLiteral("FLUSHALL");
    private static final RespArrayElementsWriter FLUSHDB = new BulkStringLiteral("FLUSHDB");
    private static final RespArrayElementsWriter GET = new BulkStringLiteral("GET");
    private static final RespArrayElementsWriter HDEL = new BulkStringLiteral("HDEL");
    private static final RespArrayElementsWriter HGET = new BulkStringLiteral("HGET");
    private static final RespArrayElementsWriter HGETALL = new BulkStringLiteral("HGETALL");
    private static final RespArrayElementsWriter HINCRBY = new BulkStringLiteral("HINCRBY");
    private static final RespArrayElementsWriter HKEYS = new BulkStringLiteral("HKEYS");
    private static final RespArrayElementsWriter HLEN = new BulkStringLiteral("HLEN");
    private static final RespArrayElementsWriter HMGET = new BulkStringLiteral("HMGET");
    private static final RespArrayElementsWriter HMSET = new BulkStringLiteral("HMSET");
    private static final RespArrayElementsWriter HSET = new BulkStringLiteral("HSET");
    private static final RespArrayElementsWriter SADD = new BulkStringLiteral("SADD");
    private static final RespArrayElementsWriter SET = new BulkStringLiteral("SET");
    private static final RespArrayElementsWriter SETNX = new BulkStringLiteral("SETNX");
    private static final RespArrayElementsWriter SMEMBERS = new BulkStringLiteral("SMEMBERS");
    private static final RespArrayElementsWriter PING = new BulkStringLiteral("PING");

    public static <K, R> Command1<K, R> del(MultiEncoder<? super K> keysEncoder, ReplyParser<? extends R> replyParser) {
        return keys -> define(replyParser, DEL, keysEncoder.encode(keys));
    }

    public static <K, R> Command1<K, R> echo(Encoder<? super K> messageEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(replyParser, ECHO, messageEncoder.encode(key));
    }

    public static <R> Command<R> flushall(ReplyParser<? extends R> replyParser) {
        return define(replyParser, FLUSHALL);
    }

    public static <R> Command<R> flushdb(ReplyParser<? extends R> replyParser) {
        return define(replyParser, FLUSHDB);
    }

    public static <K, R> Command1<K, R> get(Encoder<? super K> keyEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(replyParser, GET, keyEncoder.encode(key));
    }

    public static <K, F, R> Command2<K, F, R> hdel(Encoder<? super K> keyEncoder, MultiEncoder<? super F> fieldsEncoder,
            ReplyParser<? extends R> replyParser) {
        return (key, fields) -> define(replyParser, HDEL, keyEncoder.encode(key), fieldsEncoder.encode(fields));
    }

    public static <K, F, R> Command2<K, F, R> hget(Encoder<? super K> keyEncoder, Encoder<? super F> fieldEncoder,
            ReplyParser<? extends R> replyParser) {
        return (key, field) -> define(replyParser, HGET, keyEncoder.encode(key), fieldEncoder.encode(field));
    }

    public static <K, R> Command1<K, R> hgetall(Encoder<? super K> keyEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(replyParser, HGETALL, keyEncoder.encode(key));
    }

    public static <K, R> Command1<K, R> hkeys(Encoder<? super K> keyEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(replyParser, HKEYS, keyEncoder.encode(key));
    }

    public static <K, F, V, R> Command3<K, F, V, R> hincrby(Encoder<? super K> keyEncoder,
            Encoder<? super F> fieldEncoder, Encoder<? super V> incrementEncoder,
            ReplyParser<? extends R> replyParser) {
        return (key, field, increment) -> define(replyParser, HINCRBY, keyEncoder.encode(key),
                fieldEncoder.encode(field), incrementEncoder.encode(increment));
    }

    public static <K, R> Command1<K, R> hlen(Encoder<? super K> keyEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(replyParser, HLEN, keyEncoder.encode(key));
    }

    public static <K, F, R> Command2<K, F, R> hmget(Encoder<? super K> keyEncoder,
            MultiEncoder<? super F> fieldsEncoder, ReplyParser<? extends R> replyParser) {
        return (key, fields) -> define(replyParser, HMGET, keyEncoder.encode(key), fieldsEncoder.encode(fields));
    }

    public static <K, F, R> Command2<K, F, R> hmset(Encoder<? super K> keyEncoder,
            MultiEncoder<? super F> fieldsEncoder, ReplyParser<? extends R> replyParser) {
        return (key, fields) -> define(replyParser, HMSET, keyEncoder.encode(key), fieldsEncoder.encode(fields));
    }

    public static <K, F, V, R> Command3<K, F, V, R> hset(Encoder<? super K> keyEncoder, Encoder<? super F> fieldEncoder,
            Encoder<? super V> valueEncoder, ReplyParser<? extends R> replyParser) {
        return (key, field, value) -> define(replyParser, HSET, keyEncoder.encode(key), fieldEncoder.encode(field),
                valueEncoder.encode(value));
    }

    public static <K, V, R> Command2<K, V, R> sadd(Encoder<? super K> keyEncoder, MultiEncoder<? super V> valuesEncoder,
            ReplyParser<? extends R> replyParser) {
        return (key, values) -> define(replyParser, SADD, keyEncoder.encode(key), valuesEncoder.encode(values));
    }

    public static <K, V, R> Command2<K, V, R> set(Encoder<? super K> keyEncoder, Encoder<? super V> valueEncoder,
            ReplyParser<? extends R> replyParser) {
        return (key, value) -> define(replyParser, SET, keyEncoder.encode(key), valueEncoder.encode(value));
    }

    public static <K, V, R> Command2<K, V, R> setnx(Encoder<? super K> keyEncoder, Encoder<? super V> valueEncoder,
            ReplyParser<? extends R> replyParser) {
        return (key, value) -> define(replyParser, SETNX, keyEncoder.encode(key), valueEncoder.encode(value));
    }

    public static <K, R> Command1<K, R> smembers(Encoder<? super K> keyEncoder, ReplyParser<? extends R> replyParser) {
        return key -> define(replyParser, SMEMBERS, keyEncoder.encode(key));
    }

    public static <R> Command<R> ping(ReplyParser<? extends R> replyParser) {
        return define(replyParser, PING);
    }

    private static <T> Command<T> define(ReplyParser<? extends T> parser, RespArrayElementsWriter... paramWriters) {
        return new Command<T>() {
            private final CommandWriter commandWriter = (writeBufferSupplier, charsetEncoder) -> {
                RespSink sink = new ByteBuffersRespSink(writeBufferSupplier, charsetEncoder);
                int total = 0;
                for (RespArrayElementsWriter paramWriter : paramWriters) {
                    total += paramWriter.size();
                }
                sink.array(total);
                for (RespArrayElementsWriter paramWriter : paramWriters) {
                    paramWriter.writeTo(sink);
                }
            };

            @Override
            public CommandWriter writer() {
                return commandWriter;
            }

            @Override
            public ReplyParser<? extends T> parser() {
                return parser;
            }
        };
    }
}
