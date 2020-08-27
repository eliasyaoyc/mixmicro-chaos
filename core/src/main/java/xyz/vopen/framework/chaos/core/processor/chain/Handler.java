package xyz.vopen.framework.chaos.core.processor.chain;

import xyz.vopen.framework.chaos.core.internals.request.ChaosResponse;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;

/**
 * {@link Handler}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/14
 */
public abstract class Handler {

  /** The next handler. */
  protected Handler successor;

  public abstract void handler(
          Session session, Request request, ChaosContext context);

  public Handler setSuccessor(Handler handler) {
    this.successor = handler;
    return handler;
  }

  public Handler getSuccessor() {
    return successor;
  }

}
