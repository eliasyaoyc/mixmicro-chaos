package xyz.vopen.framework.chaos.remoting.aio.buffer;

/**
 * {@link FastBufferThread}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class FastBufferThread extends Thread {

  /** 索引标识 */
  private final int index;

  FastBufferThread(Runnable target, String name, int index) {
    super(target, name + index);
    this.index = index;
  }

  public int getIndex() {
    return index;
  }
}
