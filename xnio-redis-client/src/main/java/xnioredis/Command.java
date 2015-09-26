package xnioredis;

import xnioredis.decoder.parser.ReplyParser;
import xnioredis.encoder.Encoder;
import xnioredis.encoder.RespArrayElementsWriter;

import java.util.Arrays;
import java.util.stream.Stream;

public interface Command<T> extends Request<T> {
    RespArrayElementsWriter[] writers();

    @Override
    default CommandWriter writer() {
        return (sink) -> {
            int total = 0;
            for (RespArrayElementsWriter paramWriter : writers()) {
                total += paramWriter.size();
            }
            sink.array(total);
            for (RespArrayElementsWriter paramWriter : writers()) {
                paramWriter.writeTo(sink);
            }
        };
    }

    default <V> Command<T> append(OptionalValue<V> name, V value) {
        return new Command<T>() {
            @Override
            public RespArrayElementsWriter[] writers() {
                return Stream.concat(Arrays.stream(Command.this.writers()),
                        Stream.of(name.nameWriter(), name.encoder().encode(value)))
                        .toArray(RespArrayElementsWriter[]::new);
            }

            @Override
            public ReplyParser<? extends T> parser() {
                return Command.this.parser();
            }
        };
    }

    interface OptionalValue<V> {
        RespArrayElementsWriter nameWriter();

        Encoder<V> encoder();
    }
}
