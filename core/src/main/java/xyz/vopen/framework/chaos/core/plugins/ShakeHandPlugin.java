package xyz.vopen.framework.chaos.core.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory;
import xyz.vopen.framework.chaos.remoting.aio.transport.TcpAioSession;
import xyz.vopen.framework.chaos.remoting.api.AbstractPlugin;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type.SHAKEHAND;

/**
 * {@link ShakeHandPlugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/27
 */
public class ShakeHandPlugin<T> extends AbstractPlugin<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReconnectPlugin.class);

  private ChaosContext context;

  public ShakeHandPlugin(ChaosContext context) {
    this.context = context;
  }

  @Override
  public void stateEvent(StateMachineEnum stateMachineEnum, Session session, Throwable throwable) {
    if (stateMachineEnum != StateMachineEnum.NEW_SESSION) {
      return;
    }

    try {
      context.getProtocol().send0(context.getRequestFactory().getRequest(SHAKEHAND), session);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
