package xnioredis.decoder;

import xnioredis.decoder.parser.ArrayAsMapParser;
import xnioredis.decoder.parser.ArrayParser;
import xnioredis.decoder.parser.BulkStringParser;
import xnioredis.decoder.parser.LenParser;
import xnioredis.decoder.parser.LongParser;
import xnioredis.decoder.parser.Parser;
import xnioredis.decoder.parser.PrefixedParser;
import xnioredis.decoder.parser.ReplyParser;
import xnioredis.decoder.parser.SeqParser;
import xnioredis.decoder.parser.StringParser;
import xnioredis.decoder.parser.SuccessOrFailureParser;

public class Replies {

    private static final SuccessOrFailureParser<Integer> INTEGER_REPLY_PARSER = new SuccessOrFailureParser<>(':', LongParser.INTEGER_PARSER);
    private static final SuccessOrFailureParser<Long> LONG_REPLY_PARSER = new SuccessOrFailureParser<>(':', LongParser.LONG_PARSER);
    private static final SuccessOrFailureParser<CharSequence> SIMPLE_STRING_REPLY_PARSER = new SuccessOrFailureParser<>('+', StringParser.INSTANCE);

    public static ReplyParser<Integer> integerReply() {
        return INTEGER_REPLY_PARSER;
    }

    public static ReplyParser<Long> longReply() {
        return LONG_REPLY_PARSER;
    }

    public static ReplyParser<CharSequence> simpleStringReply() {
        return SIMPLE_STRING_REPLY_PARSER;
    }

    public static <T> ReplyParser<T> bulkStringReply(BulkStringBuilderFactory<? extends T> builderFactory) {
        return new SuccessOrFailureParser<>('$', new LenParser<>(len -> new BulkStringParser<>(len, builderFactory)));
    }

    private static <T> Parser<T> bulkStringReplyNoFail(BulkStringBuilderFactory<? extends T> builderFactory) {
        return new PrefixedParser<>('$', new LenParser<>(len -> new BulkStringParser<>(len, builderFactory)));
    }

    public static <E, T> ReplyParser<T> arrayReply(
            ArrayBuilderFactory<E, ? extends T> arrayBuilderFactory,
            BulkStringBuilderFactory<? extends E> elementBuilderFactory) {
        Parser<E> elementParser = bulkStringReplyNoFail(elementBuilderFactory);
        return new SuccessOrFailureParser<>('*', new LenParser<>(len -> new ArrayParser<>(len, arrayBuilderFactory, elementParser)));
    }

    public static <K, V, T> ReplyParser<T> mapReply(
            MapBuilderFactory<K, V, ? extends T> arrayBuilderFactory,
            BulkStringBuilderFactory<? extends K> keyBuilderFactory,
            BulkStringBuilderFactory<? extends V> valueBuilderFactory) {
        SeqParser<K, V> kvParser = SeqParser.seq(bulkStringReplyNoFail(keyBuilderFactory), bulkStringReplyNoFail(valueBuilderFactory));
        return new SuccessOrFailureParser<>('*', new LenParser<>(len -> new ArrayAsMapParser<>(len / 2, arrayBuilderFactory, kvParser)));
    }
}
