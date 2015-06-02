package xnioredis.encoder;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class Encoders {
    public static Encoder<CharSequence> strArg() {
        return CommandBuilder::bulkString;
    }

    public static Encoder<Long> longArg() {
        return CommandBuilder::bulkString;
    }

    public static Encoder<Integer> intArg() {
        return CommandBuilder::bulkString;
    }

    public static Encoder<byte[]> bytesArg() {
        return CommandBuilder::bulkString;
    }

    public static <E> MultiEncoder<E[]> arrayArg(Encoder<? super E> elemEncoder) {
        return new MultiEncoder<E[]>() {
            @Override
            public int size(E[] es) {
                return es.length;
            }

            @Override
            public void write(CommandBuilder builder, E[] es) throws IOException {
                for (E e : es) {
                    elemEncoder.write(builder, e);
                }
            }
        };
    }

    public static <E> MultiEncoder<Collection<? extends E>> collArg(Encoder<? super E> elemEncoder) {
        return new MultiEncoder<Collection<? extends E>>() {
            @Override
            public int size(Collection<? extends E> es) {
                return es.size();
            }

            @Override
            public void write(CommandBuilder builder, Collection<? extends E> es) throws IOException {
                for (E e : es) {
                    elemEncoder.write(builder, e);
                }
            }
        };
    }

    public static <K, V> MultiEncoder<Map<? extends K, ? extends V>> mapArg(Encoder<? super K> keyEncoder, Encoder<? super V> valueEncoder) {
        return new MultiEncoder<Map<? extends K, ? extends V>>() {
            @Override
            public int size(Map<? extends K, ? extends V> map) {
                return map.size() * 2;
            }

            @Override
            public void write(CommandBuilder builder, Map<? extends K, ? extends V> map) throws IOException {
                for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                    keyEncoder.write(builder, entry.getKey());
                    valueEncoder.write(builder, entry.getValue());
                }
            }
        };
    }
}
