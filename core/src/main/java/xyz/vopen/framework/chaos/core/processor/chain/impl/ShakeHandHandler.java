package xyz.vopen.framework.chaos.core.processor.chain.impl;

import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.processor.chain.Handler;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;

import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type.SHAKEHAND;

/**
 * {@link ShakeHandHandler}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/27
 */
public class ShakeHandHandler extends Handler {

  @Override
  public void handler(Session session, Request request, ChaosContext context) {
    if (request.getType() == SHAKEHAND.getType()) {
      if (request instanceof ChaosRequest) {
        ChaosRequest chaosRequest = (ChaosRequest) request;
        session.setSessionId(chaosRequest.getServiceName() + "-" + chaosRequest.getClientId());
        context.getEventManager().addServiceNode(session);
      }
    }
    if (this.getSuccessor() == null) {
      return;
    }
    this.getSuccessor().handler(session, request, context);
  }
}
