package xnioredis.commands;

import xnioredis.encoder.RespArrayElementsWriter;
import xnioredis.encoder.RespSink;

import java.io.IOException;

class BulkStringLiteral implements RespArrayElementsWriter {
    private final byte[] bytes;

    public BulkStringLiteral(CharSequence str) {
        int length = str.length();
        String lengthStr = Integer.toString(length);
        int lengthStrLength = lengthStr.length();
        this.bytes = new byte[length + lengthStrLength + 5];
        bytes[0] = '$';
        int j = 1;
        for (int i = 0; i < lengthStrLength; i++) {
            bytes[j++] = (byte) lengthStr.charAt(i);
        }
        bytes[j++] = '\r';
        bytes[j++] = '\n';
        for (int i = 0; i < length; i++) {
            bytes[j++] = (byte) str.charAt(i);
        }
        bytes[j++] = '\r';
        bytes[j] = '\n';
    }

    @Override
    public void writeTo(RespSink sink) throws IOException {
        sink.writeRaw(bytes);
    }
}
