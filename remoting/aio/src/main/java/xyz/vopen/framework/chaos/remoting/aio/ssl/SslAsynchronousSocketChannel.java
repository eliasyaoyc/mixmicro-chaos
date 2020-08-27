package xyz.vopen.framework.chaos.remoting.aio.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPage;
import xyz.vopen.framework.chaos.remoting.aio.buffer.VirtualBuffer;
import xyz.vopen.framework.chaos.remoting.aio.exception.SslException;
import xyz.vopen.framework.chaos.remoting.api.ssl.HandshakeCallback;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.Pipe;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * {@link SslAsynchronousSocketChannel}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class SslAsynchronousSocketChannel extends AsynchronousSocketChannel {

  private static final Logger LOGGER = LoggerFactory.getLogger(SslAsynchronousSocketChannel.class);

  private final VirtualBuffer netWriteBuffer;
  private final VirtualBuffer netReadBuffer;
  private final VirtualBuffer appReadBuffer;
  private final AsynchronousSocketChannel asynchronousSocketChannel;
  private SSLEngine sslEngine = null;

  /** 完成握手置null */
  private HandshakeModel handshakeModel;
  /** 完成握手置null */
  private SslService sslService;

  private boolean handshake = true;
  /** 自适应的输出长度 */
  private int adaptiveWriteSize = -1;

  public SslAsynchronousSocketChannel(
      AsynchronousSocketChannel asynchronousSocketChannel,
      SslService sslService,
      BufferPage bufferPage) {
    super(null);
    this.handshakeModel = sslService.createSSLEngine(asynchronousSocketChannel, bufferPage);
    this.sslService = sslService;
    this.asynchronousSocketChannel = asynchronousSocketChannel;
    this.sslEngine = handshakeModel.getSslEngine();
    this.netWriteBuffer = handshakeModel.getNetWriteBuffer();
    this.netReadBuffer = handshakeModel.getNetReadBuffer();
    this.appReadBuffer = handshakeModel.getAppReadBuffer();
  }

  @Override
  public AsynchronousSocketChannel bind(SocketAddress local) throws IOException {
    return asynchronousSocketChannel.bind(local);
  }

  @Override
  public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
    return asynchronousSocketChannel.setOption(name, value);
  }

  @Override
  public <T> T getOption(SocketOption<T> name) throws IOException {
    return asynchronousSocketChannel.getOption(name);
  }

  @Override
  public Set<SocketOption<?>> supportedOptions() {
    return asynchronousSocketChannel.supportedOptions();
  }

  @Override
  public AsynchronousSocketChannel shutdownInput() throws IOException {
    return asynchronousSocketChannel.shutdownInput();
  }

  @Override
  public AsynchronousSocketChannel shutdownOutput() throws IOException {
    return asynchronousSocketChannel.shutdownOutput();
  }

  @Override
  public SocketAddress getRemoteAddress() throws IOException {
    return asynchronousSocketChannel.getRemoteAddress();
  }

  @Override
  public <A> void connect(
      SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
    asynchronousSocketChannel.connect(remote, attachment, handler);
  }

  @Override
  public Future<Void> connect(SocketAddress remote) {
    return asynchronousSocketChannel.connect(remote);
  }

  @Override
  public <A> void read(
      ByteBuffer dst,
      long timeout,
      TimeUnit unit,
      A attachment,
      CompletionHandler<Integer, ? super A> handler) {
    if (handshake) {
      handshakeModel.setHandshakeCallback(
          new HandshakeCallback() {
            @Override
            public void callback() {
              handshake = false;
              synchronized (SslAsynchronousSocketChannel.this) {
                // release memory.
                handshakeModel.getAppWriteBuffer().clean();
                netReadBuffer.buffer().clear();
                netWriteBuffer.buffer().clear();
                appReadBuffer.buffer().clear();
                SslAsynchronousSocketChannel.this.notifyAll();
              }
              if (handshakeModel.isEof()) {
                handler.completed(-1, attachment);
              } else {
                SslAsynchronousSocketChannel.this.read(dst, timeout, unit, attachment, handler);
              }
              handshakeModel = null; // help gc.
            }
          });
      // trigger handshake.
      sslService.doHandshake(handshakeModel);
      return;
    }
    ByteBuffer appBuffer = appReadBuffer.buffer();
    if (appBuffer.hasRemaining()) {
      int pos = dst.position();
      if (appBuffer.remaining() > dst.remaining()) {
        int limit = appBuffer.limit();
        appBuffer.limit(appBuffer.position() + dst.remaining());
        dst.put(appBuffer);
        appBuffer.limit(limit);
      } else {
        dst.put(appBuffer);
      }
      handler.completed(dst.position() - pos, attachment);
      return;
    }
    asynchronousSocketChannel.read(
        netReadBuffer.buffer(),
        timeout,
        unit,
        attachment,
        new CompletionHandler<Integer, A>() {
          @Override
          public void completed(Integer result, A attachment) {
            int pos = dst.position();
            ByteBuffer appBuffer = appReadBuffer.buffer();
            appBuffer.clear();
            doUnWrap();
            appBuffer.flip();
            if (appBuffer.remaining() > dst.remaining()) {
              int limit = appBuffer.limit();
              appBuffer.limit(appBuffer.position() + dst.remaining());
              dst.put(appBuffer);
              appBuffer.limit(limit);
            } else if (appBuffer.hasRemaining()) {
              dst.put(appBuffer);
            } else if (result > 0) {
              appBuffer.compact();
              asynchronousSocketChannel.read(
                  netReadBuffer.buffer(), timeout, unit, attachment, this);
              return;
            }
            handler.completed(result != -1 ? dst.position() - pos : result, attachment);
          }

          @Override
          public void failed(Throwable exc, A attachment) {
            handler.failed(exc, attachment);
          }
        });
  }

  private void doUnWrap() {
    try {
      ByteBuffer netBuffer = netReadBuffer.buffer();
      ByteBuffer appBuffer = appReadBuffer.buffer();
      netBuffer.flip();
      SSLEngineResult result = sslEngine.unwrap(netBuffer, appBuffer);
      boolean closed = false;
      while (!closed && result.getStatus() != SSLEngineResult.Status.OK) {
        switch (result.getStatus()) {
          case BUFFER_OVERFLOW:
            LOGGER.warn("Buffer overflow error.");
            break;
          case BUFFER_UNDERFLOW:
            if (netBuffer.limit() == netBuffer.capacity()) {
              LOGGER.warn("Buffer underflow error.");
            } else {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("buffer underflow , continue read : {}", netBuffer);
              }
              if (netBuffer.position() > 0) {
                netBuffer.compact();
              } else {
                netBuffer.position(netBuffer.limit());
                netBuffer.limit(netBuffer.capacity());
              }
            }
            return;
          case CLOSED:
            LOGGER.warn("doUnWrap result: {}", result.getStatus());
            closed = true;
            break;
          default:
            LOGGER.warn("doUnWrap result: {}", result.getStatus());
        }
        result = sslEngine.unwrap(netBuffer, appBuffer);
      }
      netBuffer.compact();
    } catch (SSLException e) {
      throw new SslException(e);
    }
  }

  @Override
  public Future<Integer> read(ByteBuffer dst) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <A> void read(
      ByteBuffer[] dsts,
      int offset,
      int length,
      long timeout,
      TimeUnit unit,
      A attachment,
      CompletionHandler<Long, ? super A> handler) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <A> void write(
      ByteBuffer src,
      long timeout,
      TimeUnit unit,
      A attachment,
      CompletionHandler<Integer, ? super A> handler) {
    if (handshake) {
      checkInitialized();
    }
    int pos = src.position();
    doWrap(src);
    asynchronousSocketChannel.write(
        netWriteBuffer.buffer(),
        timeout,
        unit,
        attachment,
        new CompletionHandler<Integer, A>() {
          @Override
          public void completed(Integer result, A attachment) {
            if (result == -1) {
              LOGGER.error("write error");
            }
            if (netWriteBuffer.buffer().hasRemaining()) {
              asynchronousSocketChannel.write(
                  netWriteBuffer.buffer(), timeout, unit, attachment, this);
            } else {
              handler.completed(src.position() - pos, attachment);
            }
          }

          @Override
          public void failed(Throwable exc, A attachment) {
            handler.failed(exc, attachment);
          }
        });
  }

  private void checkInitialized() {
    if (!handshake) {
      return;
    }
    synchronized (this) {
      if (!handshake) {
        return;
      }
      try {
        this.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void doWrap(ByteBuffer writeBuffer) {
    try {
      ByteBuffer netBuffer = netWriteBuffer.buffer();
      netBuffer.compact();
      int limit = writeBuffer.limit();
      if (adaptiveWriteSize > 0 && writeBuffer.remaining() > adaptiveWriteSize) {
        writeBuffer.limit(writeBuffer.position() + adaptiveWriteSize);
      }
      SSLEngineResult result = sslEngine.wrap(writeBuffer, netBuffer);
      while (result.getStatus() != SSLEngineResult.Status.OK) {
        switch (result.getStatus()) {
          case BUFFER_OVERFLOW:
            netBuffer.clear();
            writeBuffer.limit(
                writeBuffer.position() + ((writeBuffer.limit() - writeBuffer.position() >> 1)));
            adaptiveWriteSize = writeBuffer.remaining();
            LOGGER.info("doWrap buffer overflow maybe size : {}", adaptiveWriteSize);
            break;
          case BUFFER_UNDERFLOW:
            LOGGER.info("doWrap buffer unOverflow");
            break;
          default:
            LOGGER.warn("doWrap result: {}", result.getStatus());
        }
        result = sslEngine.wrap(writeBuffer, netBuffer);
      }
      writeBuffer.limit(limit);
      netBuffer.flip();
    } catch (Exception e) {
      throw new SslException(e);
    }
  }

  @Override
  public Future<Integer> write(ByteBuffer src) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <A> void write(
      ByteBuffer[] srcs,
      int offset,
      int length,
      long timeout,
      TimeUnit unit,
      A attachment,
      CompletionHandler<Long, ? super A> handler) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SocketAddress getLocalAddress() throws IOException {
    return asynchronousSocketChannel.getLocalAddress();
  }

  @Override
  public boolean isOpen() {
    return asynchronousSocketChannel.isOpen();
  }

  @Override
  public void close() throws IOException {
    netWriteBuffer.clean();
    netReadBuffer.clean();
    appReadBuffer.clean();
    try{
      sslEngine.closeInbound();
    }catch (SSLException e){
      LOGGER.error("ignore closeInbound exception:{}",e.getMessage());
    }
    sslEngine.closeOutbound();
    asynchronousSocketChannel.close();
  }
}
