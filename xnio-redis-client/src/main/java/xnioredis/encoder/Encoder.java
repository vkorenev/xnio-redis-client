package xnioredis.encoder;

public interface Encoder<T> {
    RespArrayElementsWriter encode(T t);
}
