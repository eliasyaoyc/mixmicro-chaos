package xyz.vopen.framework.chaos.core.processor.chain.impl;

import org.springframework.util.ObjectUtils;
import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.request.ChaosResponse;
import xyz.vopen.framework.chaos.core.processor.chain.Handler;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;

import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type.INIT;

/**
 * {@link InitializationResHandler}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/18
 */
public class InitializationResHandler extends Handler {

  @Override
  public void handler(Session session, Request request, ChaosContext context) {
    if (request.getType() == INIT.getType() && context.getAccumulator() != null) {
      ChaosResponse chaosResponse = (ChaosResponse) request;
      ChaosRequest chaosRequest = context.getAccumulator().getChaosRequest(chaosResponse);

      if (chaosRequest != null && chaosRequest.getWatcher() != null) {
        // callback.
        chaosRequest
            .getWatcher()
            .initializer(
                request.getRequestId(),
                ObjectUtils.isEmpty(chaosResponse.getObj()) ? null : chaosResponse.getObj());
      }
    }
    if (this.getSuccessor() == null) {
      return;
    }
    this.getSuccessor().handler(session, request, context);
  }
}
