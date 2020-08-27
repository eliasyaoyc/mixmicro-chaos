package xyz.vopen.framework.chaos.remoting.aio.transport;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * {@link ConcurrentReadCompletionHandler}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class ConcurrentReadCompletionHandler<T> extends ReadCompletionHandler<T> {

  private Semaphore semaphore;

  private ThreadLocal<ConcurrentReadCompletionHandler> threadLocal = new ThreadLocal<>();

  private LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
  private ExecutorService executorService =
      new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, taskQueue);

  public ConcurrentReadCompletionHandler(final Semaphore semaphore) {
    this.semaphore = semaphore;
  }

  @Override
  public void completed(final Integer result, final TcpAioSession<T> tcpAioSession) {
    if (threadLocal.get() != null) {
      super.completed(result, tcpAioSession);
      return;
    }
    if (semaphore.tryAcquire()) {
      threadLocal.set(this);
      // process current callback task.
      super.completed(result, tcpAioSession);
      Runnable task;
      while ((task = taskQueue.poll()) != null) {
        task.run();
      }
      semaphore.release();
      threadLocal.set(null); // help gc.
      return;
    }

    executorService.execute(
        () -> {
          super.completed(result, tcpAioSession);
        });
  }

  @Override
  public void shutdown() {
    executorService.shutdown();
  }
}
