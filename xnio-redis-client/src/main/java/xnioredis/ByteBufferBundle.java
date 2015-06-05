package xnioredis;

import org.xnio.Pool;
import org.xnio.Pooled;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Supplier;

class ByteBufferBundle implements Supplier<ByteBuffer> {
    private final Pool<ByteBuffer> pool;
    private final Deque<Pooled<ByteBuffer>> allocated = new LinkedList<>();
    private ByteBuffer currentWriteBuffer = null;

    ByteBufferBundle(Pool<ByteBuffer> pool) {
        this.pool = pool;
    }

    @Override
    public ByteBuffer get() {
        if (currentWriteBuffer != null) {
            if (currentWriteBuffer.hasRemaining()) {
                return currentWriteBuffer;
            } else {
                currentWriteBuffer.flip();
            }
        }
        Pooled<ByteBuffer> pooledBuffer = pool.allocate();
        allocated.add(pooledBuffer);
        currentWriteBuffer = pooledBuffer.getResource();
        return currentWriteBuffer;
    }

    void startReading() {
        if (currentWriteBuffer != null) {
            currentWriteBuffer.flip();
        }
        currentWriteBuffer = null;
    }

    ByteBuffer[] getReadBuffers() {
        return allocated.stream().map(Pooled::getResource).toArray(ByteBuffer[]::new);
    }

    void startWriting() {
        Iterator<Pooled<ByteBuffer>> iterator = allocated.iterator();
        while (iterator.hasNext()) {
            Pooled<ByteBuffer> pooledBuffer = iterator.next();
            ByteBuffer byteBuffer = pooledBuffer.getResource();
            if (!byteBuffer.hasRemaining()) {
                byteBuffer.clear();
                pooledBuffer.free();
                iterator.remove();
            } else {
                break;
            }
        }
        Pooled<ByteBuffer> lastPooledBuffer = allocated.peekLast();
        if (lastPooledBuffer != null) {
            ByteBuffer lastBuffer = lastPooledBuffer.getResource();
            if (lastBuffer.limit() < lastBuffer.capacity()) {
                currentWriteBuffer = lastBuffer.compact();
            }
        }
    }

    boolean isEmpty() {
        return allocated.isEmpty();
    }

    int allocSize() {
        return allocated.size();
    }
}
