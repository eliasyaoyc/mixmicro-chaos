package xyz.vopen.framework.chaos.common.utilities;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * {@link IOUtility}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class IOUtility {

  /** 是否windows系统 */
  public static final boolean OS_WINDOWS =
      System.getProperty("os.name").toLowerCase().startsWith("windows");

  /** @param channel 需要被关闭的通道 */
  public static void close(AsynchronousSocketChannel channel) {
    if (channel == null) {
      throw new NullPointerException();
    }
    try {
      if (channel.isOpen()){
        channel.shutdownInput();
        channel.shutdownOutput();
        channel.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
