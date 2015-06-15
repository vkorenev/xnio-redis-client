package xnioredis.encoder;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class Encoders {
    public static Encoder<CharSequence> strArg() {
        return str -> sink -> sink.bulkString(str);
    }

    public static Encoder<Long> longArg() {
        return num -> sink -> sink.bulkString(num);
    }

    public static Encoder<Integer> intArg() {
        return num -> sink -> sink.bulkString(num);
    }

    public static Encoder<byte[]> bytesArg() {
        return bytes -> sink -> sink.bulkString(bytes);
    }

    public static MultiEncoder<long[]> longArrayArg() {
        return es -> new RespArrayElementsWriter() {
            @Override
            public int size() {
                return es.length;
            }

            @Override
            public void writeTo(RespSink sink) throws IOException {
                for (long e : es) {
                    sink.bulkString(e);
                }
            }
        };
    }

    public static MultiEncoder<int[]> intArrayArg() {
        return es -> new RespArrayElementsWriter() {
            @Override
            public int size() {
                return es.length;
            }

            @Override
            public void writeTo(RespSink sink) throws IOException {
                for (int e : es) {
                    sink.bulkString(e);
                }
            }
        };
    }

    public static <E> MultiEncoder<E[]> arrayArg(Encoder<? super E> elemEncoder) {
        return es -> new RespArrayElementsWriter() {
            @Override
            public int size() {
                return es.length;
            }

            @Override
            public void writeTo(RespSink sink) throws IOException {
                for (E e : es) {
                    elemEncoder.encode(e).writeTo(sink);
                }
            }
        };
    }

    public static <E> MultiEncoder<Collection<? extends E>> collArg(Encoder<? super E> elemEncoder) {
        return es -> new RespArrayElementsWriter() {
            @Override
            public int size() {
                return es.size();
            }

            @Override
            public void writeTo(RespSink sink) throws IOException {
                for (E e : es) {
                    elemEncoder.encode(e).writeTo(sink);
                }
            }
        };
    }

    public static <K, V> MultiEncoder<Map<? extends K, ? extends V>> mapArg(Encoder<? super K> keyEncoder,
            Encoder<? super V> valueEncoder) {
        return map -> new RespArrayElementsWriter() {
            @Override
            public int size() {
                return map.size() * 2;
            }

            @Override
            public void writeTo(RespSink sink) throws IOException {
                for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                    keyEncoder.encode(entry.getKey()).writeTo(sink);
                    valueEncoder.encode(entry.getValue()).writeTo(sink);
                }
            }
        };
    }
}
