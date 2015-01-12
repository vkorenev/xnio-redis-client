package xnioredis.decoder;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Function;

public interface BulkStringBuilderFactory<T> {
    Builder<T> create(int length);

    default <R> BulkStringBuilderFactory<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return length -> {
            Builder<T> builder = create(length);
            return new Builder<R>() {
                @Override
                public void append(ByteBuffer buffer) {
                    builder.append(buffer);
                }

                @Override
                public void appendLast(ByteBuffer buffer) {
                    builder.appendLast(buffer);
                }

                @Override
                public R build() {
                    return mapper.apply(builder.build());
                }
            };
        };
    }

    interface Builder<T> {
        void append(ByteBuffer buffer);

        void appendLast(ByteBuffer buffer);

        T build();
    }
}
