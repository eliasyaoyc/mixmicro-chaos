package xyz.vopen.framework.chaos.remoting.aio.decoder;

import xyz.vopen.framework.chaos.remoting.api.Decoder;

import java.nio.ByteBuffer;

/**
 * {@link FixedLengthFrameDecoder}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class FixedLengthFrameDecoder implements Decoder {

  private ByteBuffer buffer;
  private boolean finishRead;

  public FixedLengthFrameDecoder(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("frame length must be a positive integer: " + length);
    } else {
      buffer = ByteBuffer.allocate(length);
    }
  }

  @Override
  public boolean decode(ByteBuffer byteBuffer) {
    if (finishRead) {
      throw new RuntimeException("delimiter has finish read.");
    }
    if (buffer.remaining() >= byteBuffer.remaining()) {
      buffer.put(byteBuffer);
    } else {
      int limit = byteBuffer.limit();
      byteBuffer.limit(byteBuffer.position() + buffer.remaining());
      buffer.put(byteBuffer);
      byteBuffer.limit(limit);
    }
    if (buffer.hasRemaining()) {
      return false;
    }
    buffer.flip();
    finishRead = true;
    return true;
  }

  @Override
  public ByteBuffer getBuffer() {
    return this.buffer;
  }
}
