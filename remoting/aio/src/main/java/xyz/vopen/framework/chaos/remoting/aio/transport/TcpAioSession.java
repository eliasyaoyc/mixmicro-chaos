package xyz.vopen.framework.chaos.remoting.aio.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.common.utilities.IOUtility;
import xyz.vopen.framework.chaos.remoting.aio.ChaosServerConfig;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPage;
import xyz.vopen.framework.chaos.remoting.aio.buffer.VirtualBuffer;
import xyz.vopen.framework.chaos.remoting.api.AbstractAioSession;
import xyz.vopen.framework.chaos.remoting.api.Buffer;
import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.NetMonitor;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;
import xyz.vopen.framework.chaos.remoting.api.exception.RemotingException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * {@link TcpAioSession}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class TcpAioSession<T> extends AbstractAioSession {

  private static final Logger LOGGER = LoggerFactory.getLogger(TcpAioSession.class);

  /** AIO Channel. */
  private final AsynchronousSocketChannel channel;

  /** buffer for read. */
  private final VirtualBuffer readBuffer;

  /** outputStream. */
  private final WriterBuffer byteBuf;

  /** output semaphore, prevents concurrent write from causing an exception. */
  private final Semaphore semaphore = new Semaphore(1);

  /** read callback. */
  private final ReadCompletionHandler<T> readCompletionHandler;

  /** write callback. */
  private final WriteCompletionHandler<T> writeCompletionHandler;

  private final ChaosServerConfig serverConfig;

  private VirtualBuffer writeBuffer;

  private InputStream inputStream;

  private InetSocketAddress localAddress;

  private InetSocketAddress remoteAddress;

  private String sessionId = "aioSession";

  /** output data function. */
  private Function<WriterBuffer, Void> flushFunction =
      new Function<WriterBuffer, Void>() {
        @Override
        public Void apply(WriterBuffer buffer) {
          if (!semaphore.tryAcquire()) {
            return null;
          }
          TcpAioSession.this.writeBuffer = buffer.poll();
          if (writeBuffer == null) {
            semaphore.release();
          } else {
            continueWrite(writeBuffer);
          }
          return null;
        }
      };

  public TcpAioSession(
      AsynchronousSocketChannel channel,
      WriteCompletionHandler<T> writeCompletionHandler,
      BufferPage bufferPage,
      ReadCompletionHandler<T> readCompletionHandler,
      final ChaosServerConfig serverConfig) {
    this.channel = channel;
    this.readCompletionHandler = readCompletionHandler;
    this.writeCompletionHandler = writeCompletionHandler;
    this.serverConfig = serverConfig;
    this.readBuffer = bufferPage.allocate(serverConfig.getWriteBufferCapacity());
    this.byteBuf =
        new WriterBuffer(
            bufferPage,
            flushFunction,
            serverConfig.getWriteBufferSize(),
            serverConfig.getWriteBufferCapacity());

    try {
      this.localAddress = (InetSocketAddress) channel.getLocalAddress();
      this.remoteAddress = (InetSocketAddress) channel.getRemoteAddress();
    } catch (Exception e) {
      e.printStackTrace();
    }
    // trigger state machine.
    serverConfig.getProcessor().stateEvent(this, StateMachineEnum.NEW_SESSION, null);
  }

  /** initialize {@link AbstractAioSession} */
  public void initSession() {
    continueRead();
  }

  /** trigger the written operation of aio. */
  protected void writeCompleted() {
    if (writeBuffer == null) {
      writeBuffer = byteBuf.poll();
    } else if (!writeBuffer.buffer().hasRemaining()) {
      writeBuffer.clean();
      writeBuffer = byteBuf.poll();
    }
    if (writeBuffer != null) {
      continueWrite(writeBuffer);
      return;
    }
    semaphore.release();
    if (status != SESSION_STATUS_ENABLED) {
      close();
    } else {
      byteBuf.flush();
    }
  }

  /**
   * inner method, trigger the readable operation of channel.
   *
   * @param buffer
   */
  private final void readFromChannel0(ByteBuffer buffer) {
    channel.read(buffer, this, readCompletionHandler);
  }

  /**
   * inner method that trigger the written operation of channel.
   *
   * @param buffer
   */
  private final void writeToChannel0(ByteBuffer buffer) {
    channel.write(buffer, 0L, TimeUnit.MILLISECONDS, this, writeCompletionHandler);
  }

  /**
   * whether immediate close session.
   *
   * @param immediate
   */
  @Override
  public synchronized void close(boolean immediate) {
    if (status == SESSION_STATUS_CLOSED) {
      LOGGER.info("ignore, session : {} is closed.", getSessionId());
      return;
    }
    status = immediate ? SESSION_STATUS_CLOSED : SESSION_STATUS_CLOSING;
    if (immediate) {
      byteBuf.close();
      readBuffer.clean();
      if (writeBuffer != null) {
        writeBuffer.clean();
        writeBuffer = null; // help gc.
      }
      IOUtility.close(channel);
      serverConfig.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSED, null);
    } else if ((writeBuffer == null
        || !writeBuffer.buffer().hasRemaining() && !byteBuf.hasData())) {
      close(true);
    } else {
      serverConfig.getProcessor().stateEvent(this, StateMachineEnum.SESSION_CLOSING, null);
      byteBuf.flush();
    }
  }

  @Override
  public String getSessionId() {
    return this.sessionId;
  }

  @Override
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public boolean isInvalid() {
    return status != SESSION_STATUS_ENABLED;
  }

  /**
   * trigger channel read callback.
   *
   * @param eof
   */
  void readCompleted(boolean eof) {
    if (status == SESSION_STATUS_CLOSED) {
      return;
    }
    final ByteBuffer readBuffer = this.readBuffer.buffer();
    readBuffer.flip();
    final MessageProcessor<T> messageProcessor = serverConfig.getProcessor();
    while (readBuffer.hasRemaining() && status == SESSION_STATUS_ENABLED) {
      T dataEntry = null;
      try {
        dataEntry = (T) serverConfig.getProtocol().decode(readBuffer, this);
      } catch (Exception e) {
        messageProcessor.stateEvent(this, StateMachineEnum.DECODE_EXCEPTION, e);
        throw e;
      }
      if (dataEntry == null) {
        break;
      }
      // process msg.
      try {
        messageProcessor.process(this, dataEntry);
      } catch (Exception e) {
        e.printStackTrace();
        messageProcessor.stateEvent(this, StateMachineEnum.PROCESS_EXCEPTION, e);
      }
    }

    if (eof || status == SESSION_STATUS_CLOSING) {
      close(false);
      messageProcessor.stateEvent(this, StateMachineEnum.INPUT_SHUTDOWN, null);
      return;
    }
    if (status == SESSION_STATUS_CLOSED) {
      return;
    }
    if (semaphore.availablePermits() > 0) {
      byteBuf.flush();
    }
    // read data complete.
    if (readBuffer.remaining() == 0) {
      readBuffer.clear();
    } else if (readBuffer.position() > 0) {
      // only read data happened, reduce the memory copy.
      readBuffer.compact();
    } else {
      readBuffer.position(readBuffer.limit());
      readBuffer.limit(readBuffer.capacity());
    }

    // buffer overflow.
    if (!readBuffer.hasRemaining()) {
      RemotingException ex = new RemotingException("readBuffer overflow.");
      messageProcessor.stateEvent(this, StateMachineEnum.DECODE_EXCEPTION, ex);
      throw ex;
    }
    continueRead();
  }

  /** trigger read operation. */
  private void continueRead() {
    NetMonitor<T> monitor = getServerConfig().getMonitor();
    if (monitor != null) {
      monitor.beforeRead(this);
    }
    readFromChannel0(readBuffer.buffer());
  }

  /**
   * trigger write operation.
   *
   * @param writeBuffer
   */
  private void continueWrite(VirtualBuffer writeBuffer) {
    NetMonitor<T> monitor = getServerConfig().getMonitor();
    if (monitor != null) {
      monitor.beforeWrite(this);
    }
    writeToChannel0(writeBuffer.buffer());
  }

  /**
   * read data in synchronize.
   *
   * @return
   * @throws IOException
   */
  private int synRead() throws IOException {
    ByteBuffer buffer = readBuffer.buffer();
    if (buffer.remaining() > 0) {
      return 0;
    }
    try {
      buffer.clear();
      int size = channel.read(buffer).get();
      buffer.flip();
      return size;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * output stream.
   *
   * @return
   */
  @Override
  public Buffer writeBuffer() {
    return this.byteBuf;
  }

  @Override
  public InetSocketAddress getLocalAddress() throws IOException {
    if (this.localAddress != null) {
      return this.localAddress;
    }
    return null;
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    if (this.remoteAddress != null) {
      return this.remoteAddress;
    }
    return null;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return inputStream == null ? getInputStream(-1) : inputStream;
  }

  @Override
  public InputStream getInputStream(int length) throws IOException {
    if (inputStream != null) {
      throw new IOException("pre inputStream has not closed.");
    }
    if (inputStream != null) {
      return inputStream;
    }
    synchronized (this) {
      if (inputStream == null) {
        inputStream = new InnerInputStream(length);
      }
    }
    return inputStream;
  }

  ChaosServerConfig getServerConfig() {
    return this.serverConfig;
  }

  private class InnerInputStream extends InputStream {
    private int remainLength;

    InnerInputStream(int remainLength) {
      this.remainLength = remainLength >= 0 ? remainLength : -1;
    }

    @Override
    public int read() throws IOException {
      if (remainLength == 0) {
        return -1;
      }
      ByteBuffer readBuffer = TcpAioSession.this.readBuffer.buffer();
      if (readBuffer.hasRemaining()) {
        remainLength--;
        return readBuffer.get();
      }
      if (synRead() == -1) {
        remainLength = 0;
      }
      return read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (b == null) {
        throw new NullPointerException();
      } else if (off < 0 || len < 0 || len > b.length - off) {
        throw new IndexOutOfBoundsException();
      } else if (len == 0) {
        return 0;
      }
      if (remainLength == 0) {
        return -1;
      }
      if (remainLength > 0 && remainLength < len) {
        len = remainLength;
      }
      ByteBuffer readBuffer = TcpAioSession.this.readBuffer.buffer();
      int size = 0;
      while (len > 0 && synRead() != -1) {
        int readSize = readBuffer.remaining() < len ? readBuffer.remaining() : len;
        readBuffer.get(b, off + size, readSize);
        size += readSize;
        len -= readSize;
      }
      remainLength -= size;
      return size;
    }

    @Override
    public int available() throws IOException {
      if (remainLength == 0) {
        return 0;
      }
      if (synRead() == -1) {
        remainLength = 0;
        return remainLength;
      }
      ByteBuffer readBuffer = TcpAioSession.this.readBuffer.buffer();
      if (remainLength < -1) {
        return readBuffer.remaining();
      } else {
        return remainLength > readBuffer.remaining() ? readBuffer.remaining() : remainLength;
      }
    }

    @Override
    public void close() {
      if (TcpAioSession.this.inputStream == InnerInputStream.this) {
        TcpAioSession.this.inputStream = null;
      }
    }
  }
}
