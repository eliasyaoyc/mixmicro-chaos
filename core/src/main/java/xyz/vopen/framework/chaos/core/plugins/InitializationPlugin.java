package xyz.vopen.framework.chaos.core.plugins;

import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.remoting.api.AbstractPlugin;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

/**
 * {@link InitializationPlugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/29
 */
public class InitializationPlugin extends AbstractPlugin {

  private ChaosContext context;

  public InitializationPlugin(ChaosContext context) {
    this.context = context;
  }

  @Override
  public void stateEvent(StateMachineEnum stateMachineEnum, Session session, Throwable throwable) {
    if (stateMachineEnum != StateMachineEnum.NEW_SESSION) {
      return;
    }

    try {
      if (this.context.getConfig().isCoordinator()){
        // coordinator client. send additional sync request.
        sync();
      }else {
        // normal client initializes all request.
        reset();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Normal client Initializes all requests for resend when reconnection is encountered and
   * re-register heartbeat task.
   */
  private void reset() {
    this.context.getAccumulator().reset();
    this.context.wakeupNotFull();
  }

  /** Coordinator client send additional sync request for metadata synchronization. */
  private void sync() {
//    context.getProtocol().send0(context.getRequestFactory().getRequest(SHAKEHAND), session);
  }
}
