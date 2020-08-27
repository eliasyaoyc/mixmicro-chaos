package xyz.vopen.framework.chaos.core.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.common.utilities.TimerTaskUtility;
import xyz.vopen.framework.chaos.remoting.api.AbstractPlugin;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * {@link HeartBeatPlugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public abstract class HeartBeatPlugin<T> extends AbstractPlugin<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatPlugin.class);

  private Map<Session, Long> sessionMap = new HashMap<>();
  private long heartBeatRate;
  private long timeout;
  private TimeoutCallback timeoutCallback;

  private static final TimeoutCallback DEFAULT_TIMEOUT_CALLBACK =
      new TimeoutCallback() {
        @Override
        public void callback(Session session, long lastTime) {
          session.close(true);
        }
      };

  public HeartBeatPlugin(long heartBeatRate, TimeUnit timeUnit) {
    this(heartBeatRate, 0, timeUnit);
  }

  public HeartBeatPlugin(long heartBeatRate, long timeout, TimeUnit timeUnit) {
    this(heartBeatRate, timeout, timeUnit, DEFAULT_TIMEOUT_CALLBACK);
  }

  public HeartBeatPlugin(
      long heartBeatRate, long timeout, TimeUnit timeUnit, TimeoutCallback timeoutCallback) {
    if (timeout > 0 && heartBeatRate >= timeout) {
      throw new IllegalArgumentException("heartBeat Rate must little then timeout.");
    }
    this.heartBeatRate = timeUnit.toMillis(heartBeatRate);
    this.timeout = timeUnit.toMillis(timeout);
    this.timeoutCallback = timeoutCallback;
  }

  @Override
  public boolean preProcess(Session session, T t) {
    sessionMap.put(session, System.currentTimeMillis());
    if (isHeartBeatMessage(session, t)) {
      return false;
    }
    return true;
  }

  @Override
  public void stateEvent(
      StateMachineEnum stateMachineEnum, Session session, Throwable throwable) {
    switch (stateMachineEnum) {
      case NEW_SESSION:
        if (!sessionMap.containsKey(session)) {
          sessionMap.put(session, System.currentTimeMillis());
        }
        registerHeartBeat(session, heartBeatRate);
        break;
      case SESSION_CLOSED:
        sessionMap.remove(session);
        break;
      default:
        break;
    }
  }

  private void registerHeartBeat(final Session session, final long heartBeatRate) {
    if (heartBeatRate <= 0) {
      LOGGER.info("session : {} expire time : {}, stop heard monitor task", session, heartBeatRate);
      return;
    }
    LOGGER.info("session : {} register heatBeat task, expire time : {}", session, heartBeatRate);
    TimerTaskUtility.SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(
        new TimerTask() {
          @Override
          public void run() {
            if (session.isInvalid()) {
              sessionMap.remove(session);
              LOGGER.info("session : {} already expired, remove heatBeat task", session);
              return;
            }
            Long lastTime = sessionMap.get(session);
            if (lastTime == null) {
              LOGGER.warn("session : {} expired time is null");
              lastTime = System.currentTimeMillis();
              sessionMap.put(session, lastTime);
            }
            long current = System.currentTimeMillis();
            // timeout no message received , closed connection.
            if (timeout > 0 && (current - lastTime) > timeout) {
              timeoutCallback.callback(session, lastTime);

              // timeout no message received, try send heartbeat request.
            } else if (current - lastTime > heartBeatRate) {
              try {
                sendHeartRequest(session);
              } catch (IOException e) {
                LOGGER.error("heartbeat exception, will close session : {}", session);
                session.close(true);
              }
            }
          }
        },5,
        heartBeatRate,
        TimeUnit.MILLISECONDS);
  }

  public abstract void sendHeartRequest(Session session) throws IOException;

  public abstract boolean isHeartBeatMessage(Session session, T msg);

  public interface TimeoutCallback {
    void callback(Session session, long lastTime);
  }
}
