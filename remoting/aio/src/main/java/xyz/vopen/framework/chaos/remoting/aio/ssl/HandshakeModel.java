package xyz.vopen.framework.chaos.remoting.aio.ssl;

import xyz.vopen.framework.chaos.remoting.aio.buffer.VirtualBuffer;
import xyz.vopen.framework.chaos.remoting.api.ssl.HandshakeCallback;

import javax.net.ssl.SSLEngine;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * {@link HandshakeModel}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class HandshakeModel {
  private AsynchronousSocketChannel socketChannel;
  private SSLEngine sslEngine;
  private VirtualBuffer appWriteBuffer;
  private VirtualBuffer netWriteBuffer;
  private VirtualBuffer appReadBuffer;

  private VirtualBuffer netReadBuffer;
  private HandshakeCallback handshakeCallback;
  private boolean eof;
  private boolean finished;

  public HandshakeModel(
      AsynchronousSocketChannel socketChannel,
      SSLEngine sslEngine,
      VirtualBuffer appWriteBuffer,
      VirtualBuffer netWriteBuffer,
      VirtualBuffer appReadBuffer,
      VirtualBuffer netReadBuffer,
      HandshakeCallback handshakeCallback,
      boolean eof,
      boolean finished) {
    this.socketChannel = socketChannel;
    this.sslEngine = sslEngine;
    this.appWriteBuffer = appWriteBuffer;
    this.netWriteBuffer = netWriteBuffer;
    this.appReadBuffer = appReadBuffer;
    this.netReadBuffer = netReadBuffer;
    this.handshakeCallback = handshakeCallback;
    this.eof = eof;
    this.finished = finished;
  }

  public AsynchronousSocketChannel getSocketChannel() {
    return socketChannel;
  }

  public void setSocketChannel(AsynchronousSocketChannel socketChannel) {
    this.socketChannel = socketChannel;
  }

  public VirtualBuffer getAppWriteBuffer() {
    return appWriteBuffer;
  }

  public void setAppWriteBuffer(VirtualBuffer appWriteBuffer) {
    this.appWriteBuffer = appWriteBuffer;
  }

  public VirtualBuffer getNetWriteBuffer() {
    return netWriteBuffer;
  }

  public void setNetWriteBuffer(VirtualBuffer netWriteBuffer) {
    this.netWriteBuffer = netWriteBuffer;
  }

  public VirtualBuffer getAppReadBuffer() {
    return appReadBuffer;
  }

  public void setAppReadBuffer(VirtualBuffer appReadBuffer) {
    this.appReadBuffer = appReadBuffer;
  }

  public VirtualBuffer getNetReadBuffer() {
    return netReadBuffer;
  }

  public void setNetReadBuffer(VirtualBuffer netReadBuffer) {
    this.netReadBuffer = netReadBuffer;
  }

  public SSLEngine getSslEngine() {
    return sslEngine;
  }

  public void setSslEngine(SSLEngine sslEngine) {
    this.sslEngine = sslEngine;
  }

  public boolean isFinished() {
    return finished;
  }

  public void setFinished(boolean finished) {
    this.finished = finished;
  }

  public HandshakeCallback getHandshakeCallback() {
    return handshakeCallback;
  }

  public void setHandshakeCallback(HandshakeCallback handshakeCallback) {
    this.handshakeCallback = handshakeCallback;
  }

  public boolean isEof() {
    return eof;
  }

  public void setEof(boolean eof) {
    this.eof = eof;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private AsynchronousSocketChannel socketChannel;
    private SSLEngine sslEngine;
    private VirtualBuffer appWriteBuffer;
    private VirtualBuffer netWriteBuffer;
    private VirtualBuffer appReadBuffer;

    private VirtualBuffer netReadBuffer;
    private HandshakeCallback handshakeCallback;
    private boolean eof;
    private boolean finished;

    public Builder() {}

    public HandshakeModel build() {
      return new HandshakeModel(
          socketChannel,
          sslEngine,
          appWriteBuffer,
          netWriteBuffer,
          appReadBuffer,
          netReadBuffer,
          handshakeCallback,
          eof,
          finished);
    }

    public Builder socketChannel(AsynchronousSocketChannel socketChannel) {
      this.socketChannel = socketChannel;
      return this;
    }

    public Builder sslEngine(SSLEngine sslEngine) {
      this.sslEngine = sslEngine;
      return this;
    }

    public Builder appWriteBuffer(VirtualBuffer appWriteBuffer) {
      this.appWriteBuffer = appWriteBuffer;
      return this;
    }

    public Builder netWriteBuffer(VirtualBuffer netWriteBuffer) {
      this.netWriteBuffer = netWriteBuffer;
      return this;
    }

    public Builder appReadBuffer(VirtualBuffer appReadBuffer) {
      this.appReadBuffer = appReadBuffer;
      return this;
    }

    public Builder netReadBuffer(VirtualBuffer netReadBuffer) {
      this.netReadBuffer = netReadBuffer;
      return this;
    }

    public Builder handshakeCallback(HandshakeCallback handshakeCallback) {
      this.handshakeCallback = handshakeCallback;
      return this;
    }

    public Builder eof(boolean eof) {
      this.eof = eof;
      return this;
    }

    public Builder finished(boolean finished) {
      this.finished = finished;
      return this;
    }
  }
}
