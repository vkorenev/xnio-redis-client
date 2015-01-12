package xnioredis;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static xnioredis.Commands.FLUSHDB;
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
import static xnioredis.hamcrest.HasSameContentAs.hasSameContentAs;

public class RedisClientTest {
    private ClientFactory factory;
    private ListenableFuture<RedisClient> clientFuture;

    @Before
    public void openConnection() throws Exception {
        factory = new ClientFactory();
        clientFuture = factory.connect(new InetSocketAddress("localhost", 6379));
        Futures.transform(clientFuture, (AsyncFunction<RedisClient, CharSequence>) client ->
                client.send(FLUSHDB)).get();
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
        ListenableFuture<CharSequence> replyFuture = Futures.transform(clientFuture,
                (AsyncFunction<RedisClient, CharSequence>) client -> client.send(PING));
        assertThat(replyFuture.get(), hasSameContentAs("PONG"));
    }

    @Test
    public void canSendManyCommands() throws Exception {
        ListenableFuture<CharSequence> replyFuture1 = Futures.transform(clientFuture,
                (AsyncFunction<RedisClient, CharSequence>) client -> client.send(PING));
        ListenableFuture<CharSequence> replyFuture2 = Futures.transform(clientFuture,
                (AsyncFunction<RedisClient, CharSequence>) client -> client.send(PING));
        ListenableFuture<CharSequence> replyFuture3 = Futures.transform(clientFuture,
                (AsyncFunction<RedisClient, CharSequence>) client -> client.send(PING));
        assertThat(replyFuture1.get(), hasSameContentAs("PONG"));
        assertThat(replyFuture2.get(), hasSameContentAs("PONG"));
        assertThat(replyFuture3.get(), hasSameContentAs("PONG"));
    }

    @Test
    public void canFlushManually() throws Exception {
        ListenableFuture<CharSequence> replyFuture1 = Futures.transform(clientFuture,
                (AsyncFunction<RedisClient, CharSequence>) client -> client.send(PING, false));
        ListenableFuture<CharSequence> replyFuture2 = Futures.transform(clientFuture,
                (AsyncFunction<RedisClient, CharSequence>) client -> client.send(PING, false));
        ListenableFuture<CharSequence> replyFuture3 = Futures.transform(clientFuture,
                (AsyncFunction<RedisClient, CharSequence>) client -> client.send(PING, false));
        clientFuture.get().flush();
        assertThat(replyFuture1.get(), hasSameContentAs("PONG"));
        assertThat(replyFuture2.get(), hasSameContentAs("PONG"));
        assertThat(replyFuture3.get(), hasSameContentAs("PONG"));
    }

    @Test
    @SuppressWarnings({"unchecked", "varargs"})
    public void canSendListOfCommands() throws Exception {
        assertThat(
                Futures.transform(clientFuture, (AsyncFunction<RedisClient, List<CharSequence>>) client ->
                        client.send(new CommandList<>(Arrays.<Command<CharSequence>>asList(PING, PING, PING)))).get(),
                contains(hasSameContentAs("PONG"), hasSameContentAs("PONG"), hasSameContentAs("PONG")));
    }

    @Test
    public void hlenNoKey() throws Exception {
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Integer>) client ->
                client.send(HLEN, "NO_SUCH_KEY")).get(), equalTo(0));
    }

    @Test
    public void hgetNoKey() throws Exception {
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, CharSequence>) client ->
                client.send(HGET, "NO_SUCH_KEY", "FIELD")).get(), nullValue());
    }

    @Test
    public void hgetByteArrayNoKey() throws Exception {
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, byte[]>) client ->
                client.send(HGET_BYTES, "NO_SUCH_KEY", "FIELD")).get(), nullValue());
    }

    @Test
    public void hkeysNoKey() throws Exception {
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, List<CharSequence>>) client ->
                client.send(HKEYS, "NO_SUCH_KEY")).get(), empty());
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, List<CharSequence>>) client ->
                client.send(HKEYS2, "NO_SUCH_KEY")).get(), empty());
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, CharSequence[]>) client ->
                client.send(HKEYS3, "NO_SUCH_KEY")).get(), emptyArray());
    }

    @Test
    public void hincrby() throws Exception {
        String key = "H_KEY_1";
        String field = "FIELD_1";
        long a = 123L;
        long b = 9876420L;
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Integer>) client ->
                client.send(HSET_LONG, key, field, a)).get(), equalTo(1));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Long>) client ->
                client.send(HINCRBY, key, field, b)).get(), equalTo(a + b));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Long>) client ->
                client.send(HGET_LONG, key, field)).get(), equalTo(a + b));
    }

    @Test
    public void hGetSetBytes() throws Exception {
        String key = "H_KEY_1";
        String field = "FIELD_1";
        byte[] val = {'1', '2', '3'};
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Integer>) client ->
                client.send(HSET_BYTES, key, field, val)).get(), equalTo(1));
        assertArrayEquals(Futures.transform(clientFuture, (AsyncFunction<RedisClient, byte[]>) client ->
                client.send(HGET_BYTES, key, field)).get(), val);
    }

    @SuppressWarnings({"unchecked", "varargs"})
    @Test
    public void hash() throws Exception {
        String key = "H_KEY_1";
        String field1 = "FIELD_1";
        String val1 = "VAL_1";
        String field2 = "FIELD_2";
        String val2 = "VAL_2";
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Integer>) client ->
                client.send(HSET, key, field1, val1)).get(), equalTo(1));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Integer>) client ->
                client.send(HSET, key, field2, val2)).get(), equalTo(1));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Integer>) client ->
                client.send(HLEN, key)).get(), equalTo(2));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, CharSequence>) client ->
                client.send(HGET, key, field1)).get(), hasSameContentAs(val1));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, CharSequence>) client ->
                client.send(HGET, key, field2)).get(), hasSameContentAs(val2));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, CharSequence>) client ->
                client.send(HGET, key, "NO_SUCH_FIELD")).get(), nullValue());
    }

    @SuppressWarnings({"unchecked", "varargs"})
    @Test
    public void hashBulk() throws Exception {
        String key = "H_KEY_1";
        String field1 = "FIELD_1";
        String val1 = "VAL_1";
        String field2 = "FIELD_2";
        String val2 = "VAL_2";
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Map<String, CharSequence>>) client ->
                client.send(HGETALL, key)).get().entrySet(), empty());
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, CharSequence>) client ->
                client.send(HMSET, key, ImmutableMap.of(field1, val1, field2, val2))).get(), hasSameContentAs("OK"));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, List<CharSequence>>) client ->
                        client.send(HMGET, key, field1, field2, "NO_SUCH_FIELD")).get(),
                contains(hasSameContentAs(val1), hasSameContentAs(val2), nullValue()));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, List<CharSequence>>) client ->
                        client.send(HMGET2, key, Arrays.asList(field1, field2, "NO_SUCH_FIELD"))).get(),
                contains(hasSameContentAs(val1), hasSameContentAs(val2), nullValue()));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Map<String, CharSequence>>) client ->
                        client.send(HGETALL, key)).get(),
                allOf(hasEntry(equalTo(field1), hasSameContentAs(val1)), hasEntry(equalTo(field2), hasSameContentAs(val2))));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Integer>) client ->
                client.send(HDEL, key, field1, field2, "NO_SUCH_FIELD")).get(), equalTo(2));
        assertThat(Futures.transform(clientFuture, (AsyncFunction<RedisClient, Map<String, CharSequence>>) client ->
                client.send(HGETALL, key)).get().entrySet(), empty());
    }
}
