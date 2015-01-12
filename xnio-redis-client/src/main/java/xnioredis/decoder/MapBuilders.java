package xnioredis.decoder;

import com.google.common.collect.ImmutableMap;

public class MapBuilders {
    public static <K, V> MapBuilderFactory<K, V, ImmutableMap<K, V>> immutableMap() {
        return length -> new MapBuilderFactory.Builder<K, V, ImmutableMap<K, V>>() {
            private final ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

            @Override
            public void put(K key, V value) {
                builder.put(key, value);
            }

            @Override
            public ImmutableMap<K, V> build() {
                return builder.build();
            }
        };
    }
}
