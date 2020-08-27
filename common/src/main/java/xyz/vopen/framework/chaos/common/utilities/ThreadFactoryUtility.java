package xyz.vopen.framework.chaos.common.utilities;

import java.util.concurrent.ThreadFactory;

/**
 * {@link ThreadFactoryUtility}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
public class ThreadFactoryUtility {

  public static ThreadFactory createFactory(final String threadName) {
    return new ThreadFactory() {

      int sequence;

      @Override
      public Thread newThread(Runnable r) {
        sequence++;
        StringBuilder sb =
            new StringBuilder()
                .append("[ ")
                .append(Thread.currentThread().getThreadGroup().getName())
                .append(" ] ")
                .append(threadName)
                .append(" - ")
                .append(sequence);
        Thread thread = new Thread(r, sb.toString());
        thread.setDaemon(false);
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
          thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
      }
    };
  }
}
