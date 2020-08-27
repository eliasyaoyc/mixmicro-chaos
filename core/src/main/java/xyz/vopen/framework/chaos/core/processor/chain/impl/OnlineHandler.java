package xyz.vopen.framework.chaos.core.processor.chain.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.core.internals.ChaosServerContext;
import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.processor.chain.Handler;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;

import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type.ONLINE;

/**
 * {@link OnlineHandler} This handler used for process that client node initialization start. When
 * client start will send register request coordinator node is received and added to the {@link
 * xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager} idempotence. util another
 * request received to activate it and then callback.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/14
 */
public class OnlineHandler extends Handler {

  private static final Logger LOGGER = LoggerFactory.getLogger(OnlineHandler.class);

  @Override
  public void handler(Session session, Request request, ChaosContext context) {
    if (context instanceof ChaosServerContext) {
      if (request.getType() == ONLINE.getType()) {
        LOGGER.info("Chaos handler chain [ client {} register ] start...", session.getSessionId());
        if (request instanceof ChaosRequest) {
          ChaosRequest chaosRequest = (ChaosRequest) request;
          // storage event
          context.getEventManager().add(chaosRequest, session);
        }
      }
    }
    if (this.getSuccessor() == null) {
      return;
    }
    this.getSuccessor().handler(session, request, context);
  }
}
