package xnioredis;

import xnioredis.encoder.RespArrayElementsWriter;

public interface Command<T> extends Request<T> {
    RespArrayElementsWriter[] writers();

    @Override
    default CommandWriter writer() {
        return (sink) -> {
            int total = 0;
            for (RespArrayElementsWriter paramWriter : writers()) {
                total += paramWriter.size();
            }
            sink.array(total);
            for (RespArrayElementsWriter paramWriter : writers()) {
                paramWriter.writeTo(sink);
            }
        };
    }
}
