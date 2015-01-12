package xnioredis.encoder;

public interface MultiEncoder<T> extends Encoder<T> {
    int size(T t);
}
