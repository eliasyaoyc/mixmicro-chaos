package xyz.vopen.framework.chaos.core.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.request.ChaosResponse;
import xyz.vopen.framework.chaos.remoting.aio.processor.AbstractMessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

/**
 * {@link ClientMessageProcessor}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/23
 */
public class ClientMessageProcessor extends AbstractMessageProcessor<Request> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientMessageProcessor.class);

  private ChaosContext context;

  public ClientMessageProcessor(ChaosContext context) {
    this.context = context;
  }

  @Override
  public void process0(Session session, Request request) {
    LOGGER.info("Chaos client processing request");
    if (request instanceof ChaosRequest) {
      this.context.getHandler().handler(session, request, context);
    } else {
      ChaosResponse response = (ChaosResponse) request;
      this.context.getHandler().handler(session, response, context);
    }
  }

  @Override
  public void stateEvent0(Session session, StateMachineEnum stateMachineEnum, Throwable throwable) {
    switch (stateMachineEnum) {
    }
  }

  @Override
  public void destroy() {}
}
