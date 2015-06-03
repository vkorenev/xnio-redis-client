package xnioredis;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static xnioredis.Commands.DEL;
import static xnioredis.Commands.ECHO;
import static xnioredis.Commands.FLUSHDB;
import static xnioredis.Commands.GET;
import static xnioredis.Commands.HDEL;
import static xnioredis.Commands.HGET;
import static xnioredis.Commands.HGETALL;
import static xnioredis.Commands.HGET_BYTES;
import static xnioredis.Commands.HGET_LONG;
import static xnioredis.Commands.HINCRBY;
import static xnioredis.Commands.HKEYS;
import static xnioredis.Commands.HKEYS2;
import static xnioredis.Commands.HKEYS3;
import static xnioredis.Commands.HLEN;
import static xnioredis.Commands.HMGET;
import static xnioredis.Commands.HMGET2;
import static xnioredis.Commands.HMSET;
import static xnioredis.Commands.HSET;
import static xnioredis.Commands.HSET_BYTES;
import static xnioredis.Commands.HSET_LONG;
import static xnioredis.Commands.PING;
import static xnioredis.Commands.SADD;
import static xnioredis.Commands.SETNX;
import static xnioredis.Commands.SET_BYTES;
import static xnioredis.Commands.SMEMBERS;
import static xnioredis.hamcrest.HasSameContentAs.hasSameContentAs;

public class RedisClientTest {
    private ClientFactory factory;
    private ListenableFuture<RedisClient> clientFuture;

    @Before
    public void openConnection() throws Exception {
        factory = new ClientFactory(UTF_8, 1);
        clientFuture = factory.connect(new InetSocketAddress("localhost", 6379));
        RedisClient.send(clientFuture, FLUSHDB).get();
    }

    @After
    public void closeConnection() {
        Futures.getUnchecked(clientFuture).close();
        factory.close();
    }

    @Test
    public void doNothing() {
    }

    @Test
    public void canSendSingleCommand() throws Exception {
        ListenableFuture<CharSequence> replyFuture = RedisClient.send(clientFuture, PING);
        assertThat(replyFuture.get(), hasSameContentAs("PONG"));
    }

    @Test
    public void canSendManyCommands() throws Exception {
        ListenableFuture<CharSequence> replyFuture1 = RedisClient.send(clientFuture, PING);
        ListenableFuture<CharSequence> replyFuture2 = RedisClient.send(clientFuture, PING);
        ListenableFuture<CharSequence> replyFuture3 = RedisClient.send(clientFuture, PING);
        assertThat(replyFuture1.get(), hasSameContentAs("PONG"));
        assertThat(replyFuture2.get(), hasSameContentAs("PONG"));
        assertThat(replyFuture3.get(), hasSameContentAs("PONG"));
    }

    @Test
    @SuppressWarnings({"unchecked", "varargs"})
    public void canSendListOfCommands() throws Exception {
        assertThat(RedisClient.send(clientFuture,
                        new CommandList<>(Arrays.<Command<CharSequence>>asList(PING, PING, PING))).get(),
                contains(hasSameContentAs("PONG"), hasSameContentAs("PONG"), hasSameContentAs("PONG")));
    }

    @Test
    public void canSendPairOfCommands() throws Exception {
        assertThat(RedisClient.send(clientFuture,
                        new CommandPair<>(PING, PING, (str1, str2) ->
                                new StringBuilder(str1.length() + str2.length()).append(str1).append(str2))).get(),
                hasSameContentAs("PONGPONG"));
    }

    @Test
    public void echo() throws Exception {
        String message = "message";
        assertThat(RedisClient.send(clientFuture, ECHO, message).get(), hasSameContentAs(message));
    }

    @Test
    public void hlenNoKey() throws Exception {
        assertThat(RedisClient.send(clientFuture, HLEN, "NO_SUCH_KEY").get(), equalTo(0));
    }

    @Test
    public void hgetNoKey() throws Exception {
        assertThat(RedisClient.send(clientFuture, HGET, "NO_SUCH_KEY", "FIELD").get(), nullValue());
    }

    @Test
    public void hgetByteArrayNoKey() throws Exception {
        assertThat(RedisClient.send(clientFuture, HGET_BYTES, "NO_SUCH_KEY", "FIELD").get(), nullValue());
    }

    @Test
    public void hkeysNoKey() throws Exception {
        assertThat(RedisClient.send(clientFuture, HKEYS, "NO_SUCH_KEY").get(), empty());
        assertThat(RedisClient.send(clientFuture, HKEYS2, "NO_SUCH_KEY").get(), empty());
        assertThat(RedisClient.send(clientFuture, HKEYS3, "NO_SUCH_KEY").get(), emptyArray());
    }

    @Test
    public void hincrby() throws Exception {
        String key = "H_KEY_1";
        String field = "FIELD_1";
        long a = 123L;
        long b = 9876420L;
        assertThat(RedisClient.send(clientFuture, HSET_LONG, key, field, a).get(), equalTo(1));
        assertThat(RedisClient.send(clientFuture, HINCRBY, key, field, b).get(), equalTo(a + b));
        assertThat(RedisClient.send(clientFuture, HGET_LONG, key, field).get(), equalTo(a + b));
    }

    @Test
    public void hGetSetBytes() throws Exception {
        String key = "H_KEY_1";
        String field = "FIELD_1";
        byte[] val = {'1', '2', '3'};
        assertThat(RedisClient.send(clientFuture, HSET_BYTES, key, field, val).get(), equalTo(1));
        assertArrayEquals(RedisClient.send(clientFuture, HGET_BYTES, key, field).get(), val);
    }

    @Test
    public void getSetBytes() throws Exception {
        String key = "KEY_1";
        byte[] val1 = {'1', '2', '3'};
        byte[] val2 = {'4', '5', '6'};
        assertThat(RedisClient.send(clientFuture, GET, key).get(), nullValue());
        assertThat(RedisClient.send(clientFuture, SET_BYTES, key, val1).get(), hasSameContentAs("OK"));
        assertThat(RedisClient.send(clientFuture, SET_BYTES, key, val2).get(), hasSameContentAs("OK"));
        assertArrayEquals(RedisClient.send(clientFuture, GET, key).get(), val2);
    }

    @Test
    public void del() throws Exception {
        String key1 = "KEY_1";
        String key2 = "KEY_2";
        byte[] val1 = {'1', '2', '3'};
        byte[] val2 = {'4', '5', '6'};
        assertThat(RedisClient.send(clientFuture, SET_BYTES, key1, val1).get(), hasSameContentAs("OK"));
        assertThat(RedisClient.send(clientFuture, SET_BYTES, key2, val2).get(), hasSameContentAs("OK"));
        assertThat(RedisClient.send(clientFuture, DEL, key1, key2).get(), equalTo(2));
    }

    @Test
    public void getSetnxBytes() throws Exception {
        String key = "KEY_1";
        byte[] val1 = {'1', '2', '3'};
        byte[] val2 = {'4', '5', '6'};
        assertThat(RedisClient.send(clientFuture, GET, key).get(), nullValue());
        assertThat(RedisClient.send(clientFuture, SETNX, key, val1).get(), equalTo(1));
        assertThat(RedisClient.send(clientFuture, SETNX, key, val2).get(), equalTo(0));
        assertArrayEquals(RedisClient.send(clientFuture, GET, key).get(), val1);
    }

    @SuppressWarnings({"unchecked", "varargs"})
    @Test
    public void hash() throws Exception {
        String key = "H_KEY_1";
        String field1 = "FIELD_1";
        String val1 = "VAL_1";
        String field2 = "FIELD_2";
        String val2 = "VAL_2";
        assertThat(RedisClient.send(clientFuture, HSET, key, field1, val1).get(), equalTo(1));
        assertThat(RedisClient.send(clientFuture, HSET, key, field2, val2).get(), equalTo(1));
        assertThat(RedisClient.send(clientFuture, HLEN, key).get(), equalTo(2));
        assertThat(RedisClient.send(clientFuture, HGET, key, field1).get(), hasSameContentAs(val1));
        assertThat(RedisClient.send(clientFuture, HGET, key, field2).get(), hasSameContentAs(val2));
        assertThat(RedisClient.send(clientFuture, HGET, key, "NO_SUCH_FIELD").get(), nullValue());
    }

    @SuppressWarnings({"unchecked", "varargs"})
    @Test
    public void hashBulk() throws Exception {
        String key = "H_KEY_1";
        String field1 = "FIELD_1";
        String val1 = "VAL_1";
        String field2 = "FIELD_2";
        String val2 = "VAL_2";
        assertThat(RedisClient.send(clientFuture, HGETALL, key).get().entrySet(), empty());
        assertThat(RedisClient.send(clientFuture, HMSET, key, ImmutableMap.of(field1, val1, field2, val2)).get(),
                hasSameContentAs("OK"));
        assertThat(RedisClient.send(clientFuture, HMGET, key, field1, field2, "NO_SUCH_FIELD").get(),
                contains(hasSameContentAs(val1), hasSameContentAs(val2), nullValue()));
        assertThat(RedisClient.send(clientFuture, HMGET2, key, Arrays.asList(field1, field2, "NO_SUCH_FIELD")).get(),
                contains(hasSameContentAs(val1), hasSameContentAs(val2), nullValue()));
        assertThat(RedisClient.send(clientFuture, HGETALL, key).get(),
                allOf(hasEntry(equalTo(field1), hasSameContentAs(val1)),
                        hasEntry(equalTo(field2), hasSameContentAs(val2))));
        assertThat(RedisClient.send(clientFuture, HDEL, key, field1, field2, "NO_SUCH_FIELD").get(), equalTo(2));
        assertThat(RedisClient.send(clientFuture, HGETALL, key).get().entrySet(), empty());
    }

    @Test
    public void set() throws Exception {
        String key = "S_KEY_1";
        long val1 = 10;
        long val2 = 20;
        assertThat(RedisClient.send(clientFuture, SADD, key, Arrays.asList(val1, val2)).get(), equalTo(2));
        assertThat(RedisClient.send(clientFuture, SMEMBERS, key).get(), containsInAnyOrder(val1, val2));
    }
}
