package xnioredis.guava;

import xnioredis.commands.Command1;
import xnioredis.commands.Command2;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static xnioredis.commands.Hash.hgetall;
import static xnioredis.commands.Hash.hkeys;
import static xnioredis.commands.Hash.hmget;
import static xnioredis.decoder.BulkStringBuilders.charSequence;
import static xnioredis.decoder.BulkStringBuilders.string;
import static xnioredis.decoder.Replies.arrayReply;
import static xnioredis.decoder.Replies.mapReply;
import static xnioredis.encoder.Encoders.collArg;
import static xnioredis.encoder.Encoders.strArg;
import static xnioredis.guava.CollectionBuilders.immutableList;
import static xnioredis.guava.CollectionBuilders.immutableMap;

public class Commands {
    public static final Command1<CharSequence, Map<String, CharSequence>> HGETALL_G =
            hgetall(strArg(), mapReply(immutableMap(), string(), charSequence()));
    public static final Command1<CharSequence, List<CharSequence>> HKEYS_G =
            hkeys(strArg(), arrayReply(immutableList(), charSequence()));
    public static final Command2<CharSequence, Collection<? extends CharSequence>, List<CharSequence>> HMGET_G =
            hmget(strArg(), collArg(strArg()), arrayReply(immutableList(), charSequence()));
}
