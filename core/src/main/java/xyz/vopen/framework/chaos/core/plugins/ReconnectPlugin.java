package xyz.vopen.framework.chaos.core.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.core.internals.context.AbstractChaosContext.ConnectionPair;
import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.remoting.api.AbstractPlugin;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

/**
 * {@link ReconnectPlugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class ReconnectPlugin<T> extends AbstractPlugin<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReconnectPlugin.class);

  private ChaosContext context;
  private boolean shutdown = false;

  public ReconnectPlugin(ChaosContext context) {
    this.context = context;
  }

  @Override
  public void stateEvent(StateMachineEnum stateMachineEnum, Session session, Throwable throwable) {
    if (stateMachineEnum != StateMachineEnum.SESSION_CLOSED || shutdown) {
      return;
    }
    LOGGER.info("Session-id : {} reconnection.", session.getSessionId());
    try {

      if (this.context.getConfig().isCoordinator()) {
        this.context.getConnectionSet().offer(new ConnectionPair(session.getRemoteAddress(), 10));
      } else {
        this.context.replace(this.context.getConfig().getServices());
      }
    } catch (Exception e) {
      shutdown();
      e.printStackTrace();
    }
  }

  public void shutdown() {
    this.shutdown = true;
  }
}
