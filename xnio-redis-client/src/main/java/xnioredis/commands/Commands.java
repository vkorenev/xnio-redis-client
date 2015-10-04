package xnioredis.commands;

import xnioredis.Command;
import xnioredis.decoder.parser.ReplyParser;
import xnioredis.encoder.Encoder;
import xnioredis.encoder.RespArrayElementsWriter;

public class Commands {
    private static final RespArrayElementsWriter EX = new BulkStringLiteral("EX");

    protected static <T> Command<T> define(ReplyParser<? extends T> parser, RespArrayElementsWriter... paramWriters) {
        return new Command<T>() {
            @Override
            public RespArrayElementsWriter[] writers() {
                return paramWriters;
            }

            @Override
            public ReplyParser<? extends T> parser() {
                return parser;
            }
        };
    }

    public static <V> Command.OptionalValue<V> ex(Encoder<V> encoder) {
        return new Command.OptionalValue<V>() {
            @Override
            public RespArrayElementsWriter nameWriter() {
                return EX;
            }

            @Override
            public Encoder<V> encoder() {
                return encoder;
            }
        };
    }
}
