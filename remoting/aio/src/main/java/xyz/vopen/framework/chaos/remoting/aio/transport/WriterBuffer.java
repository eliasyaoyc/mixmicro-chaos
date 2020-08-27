package xyz.vopen.framework.chaos.remoting.aio.transport;

import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPage;
import xyz.vopen.framework.chaos.remoting.aio.buffer.VirtualBuffer;
import xyz.vopen.framework.chaos.remoting.api.Buffer;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.exception.RemotingException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * {@link WriterBuffer} Wraps the virtual buffer allocated to the current session to provide
 * streaming operation.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class WriterBuffer extends OutputStream implements Buffer {

  /** Stores data that is ready for output. */
  private final VirtualBuffer[] items;

  /** Synchronized lock. */
  private final ReentrantLock lock = new ReentrantLock();

  /** Condition for waiting puts. */
  private final Condition notFull = lock.newCondition();

  /** The thread blocking condition is triggered when the buffer queue is full. */
  private final Condition waiting = lock.newCondition();

  /** A cached page that provides data storage for the current {@link WriterBuffer}. */
  private final BufferPage bufferPage;

  /** Buffer data refresh function. */
  private final Function<WriterBuffer, Void> function;

  /** whether a waiting condition. */
  private volatile boolean isWaiting = false;

  /** {@link VirtualBuffer} read index. */
  private int takeIndex;

  /** {@link VirtualBuffer} write index. */
  private int putIndex;

  /** {@link VirtualBuffer} count. */
  private int count;

  /**
   * Temporary storage of the data being exported by the current biz. After the output is completed,
   * it will be stored in items.
   */
  private VirtualBuffer writeInBuf;

  /** Hold the data currently waiting to be output. */
  private volatile VirtualBuffer writeOutBuf;

  /** Is {@link WriterBuffer} currently closed. */
  private boolean closed = false;

  /** Number of cache groups to output in less than bytes. */
  private byte[] cacheByte;

  /** Default memory block size. */
  private int chunkSize;

  public WriterBuffer(
      BufferPage bufferPage, Function<WriterBuffer, Void> function, int chunkSize, int capacity) {
    this.bufferPage = bufferPage;
    this.function = function;
    this.items = new VirtualBuffer[capacity];
    this.chunkSize = chunkSize;
  }

  @Override
  public void write(int b) throws IOException {
    writeByte((byte) b);
  }

  public void writeShort(short v) throws IOException {
    initCacheBytes();
    cacheByte[0] = (byte) ((v >>> 8) & 0xFF);
    cacheByte[1] = (byte) ((v >>> 0) & 0xFF);
    write(cacheByte, 0, 2);
  }

  public void writeByte(byte b) {
    lock.lock();
    try {
      if (writeInBuf == null) {
        writeInBuf = bufferPage.allocate(chunkSize);
      }
      writeInBuf.buffer().put(b);
      if (writeInBuf.buffer().hasRemaining()) {
        return;
      }
      writeInBuf.buffer().flip();
      this.put(writeInBuf);
      writeInBuf = null;
    } finally {
      lock.unlock();
    }
    function.apply(this);
  }

  public void writeInt(int v) throws IOException {
    initCacheBytes();
    cacheByte[0] = (byte) ((v >>> 24) & 0xFF);
    cacheByte[1] = (byte) ((v >>> 16) & 0xFF);
    cacheByte[2] = (byte) ((v >>> 8) & 0xFF);
    cacheByte[3] = (byte) ((v >>> 0) & 0xFF);
    write(cacheByte, 0, 4);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (closed) {
      throw new IOException("OutputStream has closed.");
    }
    if (b == null) {
      throw new NullPointerException();
    } else if ((off < 0)
        || (off > b.length)
        || (len < 0)
        || ((off + len) > b.length)
        || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return;
    }
    lock.lock();
    try {
      waitPreWriteFinish();
      do {
        if (writeInBuf == null) {
          writeInBuf = bufferPage.allocate(Math.max(chunkSize, len - off));
        }
        ByteBuffer writeBuffer = writeInBuf.buffer();
        int minSize = Math.min(writeBuffer.remaining(), len - off);
        if (minSize == 0 || closed) {
          writeInBuf.clean();
          throw new IOException(
              "writeBuffer.remaining: " + writeBuffer.remaining() + " closed: " + closed);
        }
        writeBuffer.put(b, off, minSize);
        off += minSize;
        if (!writeBuffer.hasRemaining()) {
          writeBuffer.flip();
          VirtualBuffer buffer = writeInBuf;
          writeBuffer = null;
          this.put(buffer);
          function.apply(this);
        }
      } while (off < len);
      notifyWaiting();
    } finally {
      lock.unlock();
    }
  }

  /** wake up in the waiting thread. */
  private void notifyWaiting() {
    isWaiting = false;
    waiting.signal();
  }

  /** initializes a cache value of 8 bytes. */
  private void initCacheBytes() {
    if (cacheByte == null) {
      cacheByte = new byte[8];
    }
  }

  /** promise the sequence of the output data. */
  private void waitPreWriteFinish() throws IOException {
    while (isWaiting) {
      try {
        waiting.await();
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }
  }

  /**
   * writes and flushes the buffer.In {@link
   * xyz.vopen.framework.chaos.remoting.api.MessageProcessor#process(Session, Object)}can need not
   * call this method to perform the write operation,business execution after the completion of the
   * framework itself automatically trigger a flush. After calling this method, the data will be
   * output to the opposite side in time. If the recycle body uses this method to write data to
   * channel,the optimal performance will not be achieved.
   *
   * @param b data to be output.
   */
  public void writeAndFlush(byte[] b) throws IOException {
    if (b == null) {
      throw new NullPointerException();
    }
    writeAndFlush(b, 0, b.length);
  }

  public void writeAndFlush(byte[] b, int off, int len) throws IOException {
    write(b, off, len);
    flush();
  }

  @Override
  public void flush() {
    if (closed) {
      throw new RemotingException("OutputStream has closed.");
    }
    int size = this.count;
    if (size > 0 || writeOutBuf != null) {
      function.apply(this);
    } else if (writeInBuf != null && writeInBuf.buffer().position() > 0 && lock.tryLock()) {
      VirtualBuffer buffer = null;
      try {
        if (writeInBuf != null && writeInBuf.buffer().position() > 0) {
          buffer = writeInBuf;
          writeInBuf = null;
          buffer.buffer().flip();
          this.put(buffer);
          size++;
        }
      } finally {
        lock.unlock();
      }
      if (size > 0) {
        function.apply(this);
      }
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    lock.lock();
    try {
      flush();
      closed = true;
      VirtualBuffer byteBuf;
      while ((byteBuf = poll()) != null) {
        byteBuf.clean();
      }
      if (writeInBuf != null) {
        writeInBuf.clean();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * whether there is data to be output.
   *
   * @return
   */
  boolean hasData() {
    return count > 0 || (writeInBuf != null && writeInBuf.buffer().position() > 0);
  }

  /**
   * storage buffer to the queue for output.
   *
   * @param virtualBuffer cached object.
   */
  private void put(VirtualBuffer virtualBuffer) {
    try {
      while (count == items.length) {
        isWaiting = true;
        notFull.await();
        if (closed) {
          virtualBuffer.clean();
          return;
        }
      }
      while (writeOutBuf == null) {
        if (count > 0) {
          writeOutBuf = items[takeIndex];
          items[takeIndex] = null;
          if (++takeIndex == items.length) {
            takeIndex = 0;
          }
          count--;
        } else {
          writeOutBuf = virtualBuffer;
          return;
        }
      }
      items[putIndex] = virtualBuffer;
      if (++putIndex == items.length) {
        putIndex = 0;
      }
      count++;
    } catch (InterruptedException e) {
      throw new RemotingException(e);
    }
  }

  /**
   * Return and remove first element in buffer queue.
   *
   * @return {@link VirtualBuffer}
   */
  public VirtualBuffer poll() {
    if (writeOutBuf != null) {
      VirtualBuffer virtualBuffer = this.writeOutBuf;
      writeOutBuf = null;
      return virtualBuffer;
    }
    lock.lock();
    try {
      if (writeOutBuf != null) {
        VirtualBuffer virtualBuffer = this.writeOutBuf;
        writeOutBuf = null;
        return virtualBuffer;
      }
      if (count == 0) {
        return null;
      }
      VirtualBuffer x = items[takeIndex];
      items[takeIndex] = null;
      if (++takeIndex == items.length) {
        takeIndex = 0;
      }
      if (count-- == items.length) {
        notFull.signal();
      }
      if (count > 0) {
        writeOutBuf = items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length) {
          takeIndex = 0;
        }
        count--;
      }
      return x;
    } finally {
      lock.unlock();
    }
  }
}
