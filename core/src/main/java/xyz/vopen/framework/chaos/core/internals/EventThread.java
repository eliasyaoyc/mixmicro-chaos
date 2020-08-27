package xyz.vopen.framework.chaos.core.internals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link EventThread}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/16
 */
public class EventThread extends ChaosThread {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

  private static final String EVENT_THREAD_NAME = "eventThread";

  private final ReentrantLock lock = new ReentrantLock();

  private ChaosContext context;

  private volatile boolean running = false;

  public EventThread(ChaosContext context) {
    super(EVENT_THREAD_NAME);
    this.context = context;
    this.running = true;
  }

  @Override
  public void run() {
    lock.lock();
    try {
      while (running) {
        ChaosEventManager.ChaosRequestPair pair = this.context.getEventManager().getResponse(false);
        if (pair != null) {
          Session session = null;
          if ((session = context.getEventManager().getSession(pair.getSessionId())) != null) {
            context.getProtocol().send0(pair.getRequest(), session);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("Event thread exiting due to interrupted", e);
      this.running = false;
    } finally {
      lock.unlock();
    }
  }
}
