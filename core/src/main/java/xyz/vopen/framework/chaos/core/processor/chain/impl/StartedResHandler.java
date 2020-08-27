package xyz.vopen.framework.chaos.core.processor.chain.impl;

import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.request.ChaosResponse;
import xyz.vopen.framework.chaos.core.processor.chain.Handler;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;

import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type.STARTED;

/**
 * {@link StartedResHandler}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/18
 */
public class StartedResHandler extends Handler {

  @Override
  public void handler(Session session, Request request, ChaosContext context) {
    if (request.getType() == STARTED.getType() && context.getAccumulator() != null) {
      ChaosResponse chaosResponse = (ChaosResponse) request;
      ChaosRequest chaosRequest =
          context
              .getAccumulator()
              .getChaosRequest(chaosResponse);

      if (chaosRequest != null && chaosRequest.getWatcher() != null) {

        // callback.
        chaosRequest.getWatcher().started();
      }
    }
    if (this.getSuccessor() == null) {
      return;
    }
    this.getSuccessor().handler(session, request, context);
  }
}
