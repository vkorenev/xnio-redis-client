package xnioredis;

public class RedisException extends RuntimeException {
    public RedisException(String message) {
        super(message);
    }
}
