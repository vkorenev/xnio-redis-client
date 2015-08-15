package xnioredis;

import xnioredis.encoder.RespArrayElementsWriter;

public interface Command<T> extends Request<T>, Iterable<RespArrayElementsWriter> {
    @Override
    default CommandWriter writer() {
        return (sink) -> {
            int total = 0;
            for (RespArrayElementsWriter paramWriter : this) {
                total += paramWriter.size();
            }
            sink.array(total);
            for (RespArrayElementsWriter paramWriter : this) {
                paramWriter.writeTo(sink);
            }
        };
    }
}
