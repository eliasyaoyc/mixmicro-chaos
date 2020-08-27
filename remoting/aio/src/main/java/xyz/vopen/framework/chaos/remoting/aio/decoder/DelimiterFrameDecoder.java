package xyz.vopen.framework.chaos.remoting.aio.decoder;

import xyz.vopen.framework.chaos.remoting.aio.exception.DecoderException;
import xyz.vopen.framework.chaos.remoting.api.Decoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link DelimiterFrameDecoder}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class DelimiterFrameDecoder implements Decoder {
  private byte[] endFlag;
  private int exceptIndex;
  private List<ByteBuffer> bufferList;
  private boolean finishRead;
  private int position;

  public DelimiterFrameDecoder(byte[] endFlag, int unitBufferSize) {
    this.endFlag = endFlag;
    bufferList = new ArrayList<>();
    bufferList.add(ByteBuffer.allocate(unitBufferSize));
  }

  @Override
  public boolean decode(ByteBuffer byteBuffer) {
    if (finishRead) {
      throw new DecoderException("delimiter has finish read.");
    }
    ByteBuffer preBuffer = bufferList.get(position);

    while (byteBuffer.hasRemaining()) {
      if (!preBuffer.hasRemaining()) {
        preBuffer.flip();
        position++;
        if (position < bufferList.size()) {
          preBuffer = bufferList.get(position);
          preBuffer.clear();
        } else {
          preBuffer = ByteBuffer.allocate(preBuffer.capacity());
          bufferList.add(preBuffer);
        }
      }
      byte data = byteBuffer.get();
      preBuffer.put(data);
      if (data != endFlag[exceptIndex]) {
        exceptIndex = 0;
      } else if (++exceptIndex == endFlag.length) {
        preBuffer.flip();
        finishRead = true;
        break;
      }
    }
    return false;
  }

  @Override
  public ByteBuffer getBuffer() {
    if (position == 0) {
      return bufferList.get(position);
    }
    byte[] data =
        new byte[(position) * bufferList.get(0).capacity() + bufferList.get(position).limit()];
    int index = 0;
    for (int i = 0; i < position; i++) {
      ByteBuffer b = bufferList.get(i);
      System.arraycopy(b.array(), b.position(), data, index, b.remaining());
      index += b.remaining();
    }
    ByteBuffer lastBuffer = bufferList.get(position);
    System.arraycopy(
        lastBuffer.array(), lastBuffer.position(), data, index, lastBuffer.remaining());
    return ByteBuffer.wrap(data);
  }

  public void reset() {
    reset(null);
  }

  public void reset(byte[] endFlag) {
    if (endFlag != null) {
      this.endFlag = endFlag;
    }
    finishRead = false;
    exceptIndex = 0;
    position = 0;
    bufferList.get(position).clear();
  }
}
