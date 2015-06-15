package xnioredis.encoder;

public interface MultiEncoder<T> {
    RespArrayElementsWriter encode(T t);
}
