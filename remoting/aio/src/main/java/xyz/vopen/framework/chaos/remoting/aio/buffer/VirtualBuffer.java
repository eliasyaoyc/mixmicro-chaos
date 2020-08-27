package xyz.vopen.framework.chaos.remoting.aio.buffer;

import java.nio.ByteBuffer;

/**
 * {@link VirtualBuffer}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class VirtualBuffer {
  /** 当前虚拟buffer的归属内存页 */
  private final BufferPage bufferPage;
  /**
   * 通过ByteBuffer.slice()隐射出来的虚拟ByteBuffer
   *
   * @see ByteBuffer#slice()
   */
  private ByteBuffer buffer;
  /** 是否已回收 */
  private boolean clean = false;
  /** 当前虚拟buffer映射的实际buffer.position */
  private int parentPosition;

  /** 当前虚拟buffer映射的实际buffer.limit */
  private int parentLimit;

  VirtualBuffer(BufferPage bufferPage, ByteBuffer buffer, int parentPosition, int parentLimit) {
    this.bufferPage = bufferPage;
    this.buffer = buffer;
    this.parentPosition = parentPosition;
    this.parentLimit = parentLimit;
  }

  int getParentPosition() {
    return parentPosition;
  }

  void setParentPosition(int parentPosition) {
    this.parentPosition = parentPosition;
  }

  int getParentLimit() {
    return parentLimit;
  }

  void setParentLimit(int parentLimit) {
    this.parentLimit = parentLimit;
  }

  /**
   * 获取真实缓冲区
   *
   * @return 真实缓冲区
   */
  public ByteBuffer buffer() {
    return buffer;
  }

  /**
   * 设置真实缓冲区
   *
   * @param buffer 真实缓冲区
   */
  void buffer(ByteBuffer buffer) {
    this.buffer = buffer;
    clean = false;
  }

  /** 释放虚拟缓冲区 */
  public void clean() {
    if (clean) {
      throw new UnsupportedOperationException("buffer has cleaned");
    }
    clean = true;
    if (bufferPage != null) {
      bufferPage.clean(this);
    }
  }

  @Override
  public String toString() {
    return "VirtualBuffer{parentPosition=" + parentPosition + ", parentLimit=" + parentLimit + '}';
  }
}
