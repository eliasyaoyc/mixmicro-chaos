package xyz.vopen.framework.chaos.common.utilities;

import java.nio.ByteBuffer;

/**
 * {@link BufferUtility}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class BufferUtility {

  /** Horizontal space */
  private static final byte SP = 32;

  /** Carriage return */
  private static final byte CR = 13;

  /** Line feed character */
  private static final byte LF = 10;

  public static void trim(ByteBuffer buffer) {
    int pos = buffer.position();
    int limit = buffer.limit();

    while (pos < limit) {
      byte b = buffer.get(pos);
      if (b != SP && b != CR && b != LF) {
        break;
      }
      pos++;
    }
    buffer.position(pos);

    while (pos < limit) {
      byte b = buffer.get(limit - 1);
      if (b != SP && b != CR && b != LF) {
        break;
      }
      limit--;
    }
    buffer.limit(limit);
  }

  public static short readUnsignedByte(ByteBuffer buffer) {
    return (short) (buffer.get() & 0xFF);
  }
}
