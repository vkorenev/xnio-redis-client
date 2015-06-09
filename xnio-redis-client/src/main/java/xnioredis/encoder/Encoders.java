package xnioredis.encoder;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class Encoders {
    public static Encoder<CharSequence> strArg() {
        return RespSink::bulkString;
    }

    public static Encoder<Long> longArg() {
        return RespSink::bulkString;
    }

    public static Encoder<Integer> intArg() {
        return RespSink::bulkString;
    }

    public static Encoder<byte[]> bytesArg() {
        return RespSink::bulkString;
    }

    public static MultiEncoder<long[]> longArrayArg() {
        return new MultiEncoder<long[]>() {
            @Override
            public int size(long[] es) {
                return es.length;
            }

            @Override
            public void write(RespSink sink, long[] es) throws IOException {
                for (long e : es) {
                    sink.bulkString(e);
                }
            }
        };
    }

    public static MultiEncoder<int[]> intArrayArg() {
        return new MultiEncoder<int[]>() {
            @Override
            public int size(int[] es) {
                return es.length;
            }

            @Override
            public void write(RespSink sink, int[] es) throws IOException {
                for (int e : es) {
                    sink.bulkString(e);
                }
            }
        };
    }

    public static <E> MultiEncoder<E[]> arrayArg(Encoder<? super E> elemEncoder) {
        return new MultiEncoder<E[]>() {
            @Override
            public int size(E[] es) {
                return es.length;
            }

            @Override
            public void write(RespSink sink, E[] es) throws IOException {
                for (E e : es) {
                    elemEncoder.write(sink, e);
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
            public void write(RespSink sink, Collection<? extends E> es) throws IOException {
                for (E e : es) {
                    elemEncoder.write(sink, e);
                }
            }
        };
    }

    public static <K, V> MultiEncoder<Map<? extends K, ? extends V>> mapArg(Encoder<? super K> keyEncoder,
            Encoder<? super V> valueEncoder) {
        return new MultiEncoder<Map<? extends K, ? extends V>>() {
            @Override
            public int size(Map<? extends K, ? extends V> map) {
                return map.size() * 2;
            }

            @Override
            public void write(RespSink sink, Map<? extends K, ? extends V> map) throws IOException {
                for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                    keyEncoder.write(sink, entry.getKey());
                    valueEncoder.write(sink, entry.getValue());
                }
            }
        };
    }
}
