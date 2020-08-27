package xyz.vopen.framework.chaos.core.internals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ChaosThread} This is the main class for catching all the uncaught exceptions thrown by the
 * threads.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/16
 */
public class ChaosThread extends Thread {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChaosThread.class);

  private UncaughtExceptionHandler uncaughtExceptionHandler =
      new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
          handleException(t.getName(), e);
        }
      };

  public ChaosThread(String threadName) {
    super(threadName);
    setUncaughtExceptionHandler(uncaughtExceptionHandler);
  }

  /**
   * This will be used by the uncaught exception handler and just log a warning message and return.
   *
   * @param thName thread name.
   * @param e exception object.
   */
  protected void handleException(String thName, Throwable e) {
    LOGGER.warn("Exception occurred from thread {}", thName, e);
  }
}
