package xnioredis;

import xnioredis.commands.Command1;
import xnioredis.commands.Command2;
import xnioredis.commands.Command3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static xnioredis.commands.Commands.del;
import static xnioredis.commands.Commands.flushall;
import static xnioredis.commands.Commands.flushdb;
import static xnioredis.commands.Commands.get;
import static xnioredis.commands.Commands.hdel;
import static xnioredis.commands.Commands.hget;
import static xnioredis.commands.Commands.hgetall;
import static xnioredis.commands.Commands.hincrby;
import static xnioredis.commands.Commands.hkeys;
import static xnioredis.commands.Commands.hlen;
import static xnioredis.commands.Commands.hmget;
import static xnioredis.commands.Commands.hmset;
import static xnioredis.commands.Commands.hset;
import static xnioredis.commands.Commands.ping;
import static xnioredis.commands.Commands.sadd;
import static xnioredis.commands.Commands.set;
import static xnioredis.commands.Commands.setnx;
import static xnioredis.commands.Commands.smembers;
import static xnioredis.decoder.ArrayBuilders.array;
import static xnioredis.decoder.ArrayBuilders.collection;
import static xnioredis.decoder.ArrayBuilders.immutableList;
import static xnioredis.decoder.BulkStringBuilders._long;
import static xnioredis.decoder.BulkStringBuilders.byteArray;
import static xnioredis.decoder.BulkStringBuilders.charSequence;
import static xnioredis.decoder.BulkStringBuilders.string;
import static xnioredis.decoder.MapBuilders.immutableMap;
import static xnioredis.decoder.Replies.arrayReply;
import static xnioredis.decoder.Replies.bulkStringReply;
import static xnioredis.decoder.Replies.integerReply;
import static xnioredis.decoder.Replies.longReply;
import static xnioredis.decoder.Replies.mapReply;
import static xnioredis.decoder.Replies.simpleStringReply;
import static xnioredis.encoder.Encoders.arrayArg;
import static xnioredis.encoder.Encoders.bytesArg;
import static xnioredis.encoder.Encoders.collArg;
import static xnioredis.encoder.Encoders.longArg;
import static xnioredis.encoder.Encoders.mapArg;
import static xnioredis.encoder.Encoders.strArg;

public class Commands {
    public static final Command1<CharSequence[], Integer> DEL = del(arrayArg(strArg()), integerReply());
    public static final Command<CharSequence> FLUSHALL = flushall(simpleStringReply());
    public static final Command<CharSequence> FLUSHDB = flushdb(simpleStringReply());
    public static final Command1<CharSequence, byte[]> GET = get(strArg(), bulkStringReply(byteArray()));
    public static final Command2<CharSequence, CharSequence[], Integer> HDEL = hdel(strArg(), arrayArg(strArg()), integerReply());
    public static final Command2<CharSequence, CharSequence, CharSequence> HGET = hget(strArg(), strArg(), bulkStringReply(charSequence(UTF_8)));
    public static final Command2<CharSequence, CharSequence, byte[]> HGET_BYTES = hget(strArg(), strArg(), bulkStringReply(byteArray()));
    public static final Command2<CharSequence, CharSequence, Long> HGET_LONG = hget(strArg(), strArg(), bulkStringReply(_long()));
    public static final Command1<CharSequence, Map<String, CharSequence>> HGETALL = hgetall(strArg(), mapReply(immutableMap(), string(UTF_8), charSequence(UTF_8)));
    public static final Command3<CharSequence, CharSequence, Long, Long> HINCRBY = hincrby(strArg(), strArg(), longArg(), longReply());
    public static final Command1<CharSequence, List<CharSequence>> HKEYS = hkeys(strArg(), arrayReply(immutableList(), charSequence(UTF_8)));
    public static final Command1<CharSequence, List<CharSequence>> HKEYS2 = hkeys(strArg(), arrayReply(collection(ArrayList::new), charSequence(UTF_8)));
    public static final Command1<CharSequence, CharSequence[]> HKEYS3 = hkeys(strArg(), arrayReply(array(CharSequence[]::new), charSequence(UTF_8)));
    public static final Command1<CharSequence, Integer> HLEN = hlen(strArg(), integerReply());
    public static final Command2<CharSequence, CharSequence[], List<CharSequence>> HMGET = hmget(strArg(), arrayArg(strArg()), arrayReply(collection(ArrayList::new), charSequence(UTF_8)));
    public static final Command2<CharSequence, Collection<? extends CharSequence>, List<CharSequence>> HMGET2 = hmget(strArg(), collArg(strArg()), arrayReply(collection(ArrayList::new), charSequence(UTF_8)));
    public static final Command2<CharSequence, Map<String, ? extends CharSequence>, CharSequence> HMSET = hmset(strArg(), mapArg(strArg(), strArg()), simpleStringReply());
    public static final Command3<CharSequence, CharSequence, CharSequence, Integer> HSET = hset(strArg(), strArg(), strArg(), integerReply());
    public static final Command3<CharSequence, CharSequence, byte[], Integer> HSET_BYTES = hset(strArg(), strArg(), bytesArg(), integerReply());
    public static final Command3<CharSequence, CharSequence, Long, Integer> HSET_LONG = hset(strArg(), strArg(), longArg(), integerReply());
    public static final Command2<CharSequence, Collection<Long>, Integer> SADD = sadd(strArg(), collArg(longArg()), integerReply());
    public static final Command2<CharSequence, CharSequence, CharSequence> SET = set(strArg(), strArg(), simpleStringReply());
    public static final Command2<CharSequence, byte[], CharSequence> SET_BYTES = set(strArg(), bytesArg(), simpleStringReply());
    public static final Command2<CharSequence, Long, CharSequence> SET_LONG = set(strArg(), longArg(), simpleStringReply());
    public static final Command2<CharSequence, byte[], Integer> SETNX = setnx(strArg(), bytesArg(), integerReply());
    public static final Command1<CharSequence, Set<Long>> SMEMBERS = smembers(strArg(), arrayReply(collection(HashSet::new), _long()));
    public static final Command<CharSequence> PING = ping(simpleStringReply());
}
