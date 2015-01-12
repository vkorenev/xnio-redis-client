package xnioredis.decoder.parser;

import xnioredis.decoder.BulkStringBuilderFactory;

import java.nio.ByteBuffer;

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
    public Result<T> parse(ByteBuffer buffer) {
        return doParse(buffer, builderFactory.create(len), len, READING);
    }

    private Result<T> doParse(ByteBuffer buffer, BulkStringBuilderFactory.Builder<? extends T> builder, int len, int state) {
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
                        return new Success<>(builder.build());
                    } else {
                        throw new IllegalStateException("LF is expected");
                    }
            }
        }
        return new BulkStringPartial(builder, len, state);
    }

    private class BulkStringPartial implements Parser.Partial<T> {
        private final BulkStringBuilderFactory.Builder<? extends T> builder;
        private final int len;
        private final int state;

        public BulkStringPartial(BulkStringBuilderFactory.Builder<? extends T> builder, int len, int state) {
            this.builder = builder;
            this.len = len;
            this.state = state;
        }

        @Override
        public Result<T> parse(ByteBuffer buffer) {
            return doParse(buffer, builder, len, state);
        }
    }
}
