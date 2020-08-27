package xyz.vopen.framework.chaos.core.processor.chain.impl;

import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.processor.chain.Handler;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;

import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type.OMINOUS;

/**
 * {@link OminousHandler}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/14
 */
public class OminousHandler extends Handler {

  @Override
  public void handler(Session session, Request request, ChaosContext context) {

    if (request.getType() == OMINOUS.getType()) {
      ChaosRequest chaosRequest = (ChaosRequest) request;
      context.getEventManager().removeEvent(chaosRequest, session);
    }
    if (this.getSuccessor() == null) {
      return;
    }
    this.getSuccessor().handler(session, request, context);
  }
}
