package xnioredis.decoder;

import xnioredis.decoder.parser.ArrayAsMapParser;
import xnioredis.decoder.parser.ArrayParser;
import xnioredis.decoder.parser.ArrayReplyParser;
import xnioredis.decoder.parser.BulkStringParser;
import xnioredis.decoder.parser.BulkStringReplyParser;
import xnioredis.decoder.parser.IntegerReplyParser;
import xnioredis.decoder.parser.LenParser;
import xnioredis.decoder.parser.LongParser;
import xnioredis.decoder.parser.Parser;
import xnioredis.decoder.parser.PrefixedParser;
import xnioredis.decoder.parser.SeqParser;
import xnioredis.decoder.parser.SimpleStringReplyParser;
import xnioredis.decoder.parser.StringParser;

public class Replies {

    private static final IntegerReplyParser<Integer> INTEGER_REPLY_PARSER =
            new IntegerReplyParser<>(LongParser.INTEGER_PARSER);
    private static final IntegerReplyParser<Long> LONG_REPLY_PARSER = new IntegerReplyParser<>(LongParser.LONG_PARSER);
    private static final SimpleStringReplyParser<CharSequence> SIMPLE_STRING_REPLY_PARSER =
            new SimpleStringReplyParser<>(StringParser.INSTANCE);

    public static IntegerReplyParser<Integer> integerReply() {
        return INTEGER_REPLY_PARSER;
    }

    public static IntegerReplyParser<Long> longReply() {
        return LONG_REPLY_PARSER;
    }

    public static SimpleStringReplyParser<CharSequence> simpleStringReply() {
        return SIMPLE_STRING_REPLY_PARSER;
    }

    public static <T> BulkStringReplyParser<T> bulkStringReply(BulkStringBuilderFactory<? extends T> builderFactory) {
        return new BulkStringReplyParser<>(new LenParser<>(len -> new BulkStringParser<>(len, builderFactory)));
    }

    private static <T> Parser<T> bulkStringReplyNoFail(BulkStringBuilderFactory<? extends T> builderFactory) {
        return new PrefixedParser<>('$', new LenParser<>(len -> new BulkStringParser<>(len, builderFactory)));
    }

    public static <E, T> ArrayReplyParser<T> arrayReply(ArrayBuilderFactory<E, ? extends T> arrayBuilderFactory,
            BulkStringBuilderFactory<? extends E> elementBuilderFactory) {
        Parser<E> elementParser = bulkStringReplyNoFail(elementBuilderFactory);
        return new ArrayReplyParser<>(
                new LenParser<>(len -> new ArrayParser<>(len, arrayBuilderFactory, elementParser)));
    }

    public static <K, V, T> ArrayReplyParser<T> mapReply(MapBuilderFactory<K, V, ? extends T> arrayBuilderFactory,
            BulkStringBuilderFactory<? extends K> keyBuilderFactory,
            BulkStringBuilderFactory<? extends V> valueBuilderFactory) {
        SeqParser<K, V> kvParser =
                SeqParser.seq(bulkStringReplyNoFail(keyBuilderFactory), bulkStringReplyNoFail(valueBuilderFactory));
        return new ArrayReplyParser<>(
                new LenParser<>(len -> new ArrayAsMapParser<>(len / 2, arrayBuilderFactory, kvParser)));
    }
}
