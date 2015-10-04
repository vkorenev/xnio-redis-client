package xnioredis.encoder;

public interface MultiPairEncoder<T> {
    RespArrayElementsWriter encode(T t);
}
