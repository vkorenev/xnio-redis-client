package xnioredis.decoder.parser;

import xnioredis.decoder.BulkStringBuilderFactory;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

public class BulkStringParser<T> implements Parser<T> {
    private static final int READING = 0;
    private static final int WAITING_FOR_CR = 1;
    private static final int WAITING_FOR_LF = 2;
    private final BulkStringBuilderFactory<? extends T> builderFactory;
    private final int len;

    public BulkStringParser(int len, BulkStringBuilderFactory<? extends T> builderFactory) {
        this.len = len;
        this.builderFactory = builderFactory;
    }

    @Override
    public <U> U parse(ByteBuffer buffer, Visitor<? super T, U> visitor, CharsetDecoder charsetDecoder) {
        return doParse(buffer, visitor, builderFactory.create(len, charsetDecoder), len, READING);
    }

    private <U> U doParse(ByteBuffer buffer, Visitor<? super T, U> visitor,
            BulkStringBuilderFactory.Builder<? extends T> builder, int len, int state) {
        while (buffer.hasRemaining()) {
            switch (state) {
                case READING:
                    if (buffer.remaining() >= len) {
                        ByteBuffer src = buffer.slice();
                        src.limit(len);
                        buffer.position(buffer.position() + len);
                        builder.appendLast(src);
                        state = WAITING_FOR_CR;
                    } else {
                        len -= buffer.remaining();
                        builder.append(buffer);
                    }
                    break;
                case WAITING_FOR_CR:
                    if (buffer.get() == '\r') {
                        state = WAITING_FOR_LF;
                    } else {
                        throw new IllegalStateException("CR is expected");
                    }
                    break;
                case WAITING_FOR_LF:
                    if (buffer.get() == '\n') {
                        return visitor.success(builder.build());
                    } else {
                        throw new IllegalStateException("LF is expected");
                    }
            }
        }
        int len1 = len;
        int state1 = state;
        return visitor.partial(new Parser<T>() {
            @Override
            public <U1> U1 parse(ByteBuffer buffer, Visitor<? super T, U1> visitor, CharsetDecoder charsetDecoder) {
                return doParse(buffer, visitor, builder, len1, state1);
            }
        });
    }
}
