package xnioredis;

import xnioredis.commands.Command1;
import xnioredis.commands.Command2;
import xnioredis.commands.Command3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static xnioredis.commands.Commands.ex;
import static xnioredis.commands.Connection.echo;
import static xnioredis.commands.Connection.ping;
import static xnioredis.commands.Generic.del;
import static xnioredis.commands.Hash.hdel;
import static xnioredis.commands.Hash.hget;
import static xnioredis.commands.Hash.hgetall;
import static xnioredis.commands.Hash.hincrby;
import static xnioredis.commands.Hash.hkeys;
import static xnioredis.commands.Hash.hlen;
import static xnioredis.commands.Hash.hmget;
import static xnioredis.commands.Hash.hmset;
import static xnioredis.commands.Hash.hset;
import static xnioredis.commands.Server.flushall;
import static xnioredis.commands.Server.flushdb;
import static xnioredis.commands.Set.sadd;
import static xnioredis.commands.Set.smembers;
import static xnioredis.commands.String.get;
import static xnioredis.commands.String.set;
import static xnioredis.commands.String.setnx;
import static xnioredis.decoder.ArrayBuilders.array;
import static xnioredis.decoder.ArrayBuilders.collection;
import static xnioredis.decoder.BulkStringBuilders._long;
import static xnioredis.decoder.BulkStringBuilders.byteArray;
import static xnioredis.decoder.BulkStringBuilders.charSequence;
import static xnioredis.decoder.BulkStringBuilders.integer;
import static xnioredis.decoder.BulkStringBuilders.string;
import static xnioredis.decoder.MapBuilders.map;
import static xnioredis.decoder.Replies.arrayReply;
import static xnioredis.decoder.Replies.bulkStringReply;
import static xnioredis.decoder.Replies.integerReply;
import static xnioredis.decoder.Replies.longReply;
import static xnioredis.decoder.Replies.mapReply;
import static xnioredis.decoder.Replies.simpleStringReply;
import static xnioredis.encoder.Encoders.arrayArg;
import static xnioredis.encoder.Encoders.bytesArg;
import static xnioredis.encoder.Encoders.collArg;
import static xnioredis.encoder.Encoders.intArg;
import static xnioredis.encoder.Encoders.intArrayArg;
import static xnioredis.encoder.Encoders.longArg;
import static xnioredis.encoder.Encoders.longArrayArg;
import static xnioredis.encoder.Encoders.mapArg;
import static xnioredis.encoder.Encoders.strArg;

public class Commands {
    public static final Command1<CharSequence[], Integer> DEL = del(arrayArg(strArg()), integerReply());
    public static final Command1<CharSequence, CharSequence> ECHO = echo(strArg(), bulkStringReply(charSequence()));
    public static final Command<CharSequence> FLUSHALL = flushall(simpleStringReply());
    public static final Command<CharSequence> FLUSHDB = flushdb(simpleStringReply());
    public static final Command1<CharSequence, byte[]> GET = get(strArg(), bulkStringReply(byteArray()));
    public static final Command2<CharSequence, CharSequence[], Integer> HDEL =
            hdel(strArg(), arrayArg(strArg()), integerReply());
    public static final Command2<CharSequence, CharSequence, CharSequence> HGET =
            hget(strArg(), strArg(), bulkStringReply(charSequence()));
    public static final Command2<CharSequence, CharSequence, byte[]> HGET_BYTES =
            hget(strArg(), strArg(), bulkStringReply(byteArray()));
    public static final Command2<CharSequence, CharSequence, Long> HGET_LONG =
            hget(strArg(), strArg(), bulkStringReply(_long()));
    public static final Command1<CharSequence, Map<String, CharSequence>> HGETALL =
            hgetall(strArg(), mapReply(map(HashMap::new), string(), charSequence()));
    public static final Command3<CharSequence, CharSequence, Long, Long> HINCRBY =
            hincrby(strArg(), strArg(), longArg(), longReply());
    public static final Command1<CharSequence, List<CharSequence>> HKEYS =
            hkeys(strArg(), arrayReply(collection(ArrayList::new), charSequence()));
    public static final Command1<CharSequence, CharSequence[]> HKEYS_A =
            hkeys(strArg(), arrayReply(array(CharSequence[]::new), charSequence()));
    public static final Command1<CharSequence, Integer> HLEN = hlen(strArg(), integerReply());
    public static final Command2<CharSequence, CharSequence[], List<CharSequence>> HMGET =
            hmget(strArg(), arrayArg(strArg()), arrayReply(collection(ArrayList::new), charSequence()));
    public static final Command2<CharSequence, Collection<? extends CharSequence>, List<CharSequence>> HMGET2 =
            hmget(strArg(), collArg(strArg()), arrayReply(collection(ArrayList::new), charSequence()));
    public static final Command2<CharSequence, Map<String, ? extends CharSequence>, CharSequence> HMSET =
            hmset(strArg(), mapArg(strArg(), strArg()), simpleStringReply());
    public static final Command3<CharSequence, CharSequence, CharSequence, Integer> HSET =
            hset(strArg(), strArg(), strArg(), integerReply());
    public static final Command3<CharSequence, CharSequence, byte[], Integer> HSET_BYTES =
            hset(strArg(), strArg(), bytesArg(), integerReply());
    public static final Command3<CharSequence, CharSequence, Long, Integer> HSET_LONG =
            hset(strArg(), strArg(), longArg(), integerReply());
    public static final Command2<CharSequence, Collection<Long>, Integer> SADD =
            sadd(strArg(), collArg(longArg()), integerReply());
    public static final Command2<CharSequence, long[], Integer> SADD_LONG_ARR =
            sadd(strArg(), longArrayArg(), integerReply());
    public static final Command2<CharSequence, int[], Integer> SADD_INT_ARR =
            sadd(strArg(), intArrayArg(), integerReply());
    public static final Command2<CharSequence, CharSequence, CharSequence> SET =
            set(strArg(), strArg(), simpleStringReply());
    public static final Command2<CharSequence, byte[], CharSequence> SET_BYTES =
            set(strArg(), bytesArg(), simpleStringReply());
    public static final Command2<CharSequence, Long, CharSequence> SET_LONG =
            set(strArg(), longArg(), simpleStringReply());
    public static final Command2<CharSequence, byte[], Integer> SETNX = setnx(strArg(), bytesArg(), integerReply());
    public static final Command1<CharSequence, Set<Long>> SMEMBERS =
            smembers(strArg(), arrayReply(collection(HashSet::new), _long()));
    public static final Command1<CharSequence, List<Integer>> SMEMBERS_INTEGER_LIST =
            smembers(strArg(), arrayReply(collection(ArrayList::new), integer()));
    public static final Command<CharSequence> PING = ping(simpleStringReply());
    public static final Command.OptionalValue<Integer> EX = ex(intArg());
}
