package xyz.vopen.framework.chaos.common.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * {@link TimerTaskUtility}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public abstract class TimerTaskUtility implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(TimerTaskUtility.class);

  public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE =
      new ScheduledThreadPoolExecutor(
          1,
          new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
              Thread thread = new Thread(r, "Quick Timer");
              return thread;
            }
          });

  public TimerTaskUtility() {
    SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(
        this, getDelay(), getPeriod(), TimeUnit.MILLISECONDS);
    logger.info("Register QuickTimerTask---- " + this.getClass().getSimpleName());
  }

  public static void cancelQuickTask() {
    SCHEDULED_EXECUTOR_SERVICE.shutdown();
  }

  public static ScheduledFuture<?> scheduleAtFixedRate(
      Runnable command, long initialDelay, long period) {
    return SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(
        command, initialDelay, period, TimeUnit.MILLISECONDS);
  }

  /** 获取定时任务的延迟启动时间 */
  protected long getDelay() {
    return 0;
  }

  /**
   * 获取定时任务的执行频率
   *
   * @return
   */
  protected abstract long getPeriod();
}
