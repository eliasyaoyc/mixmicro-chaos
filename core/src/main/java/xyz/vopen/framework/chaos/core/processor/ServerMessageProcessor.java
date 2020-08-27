package xyz.vopen.framework.chaos.core.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.core.internals.EventThread;
import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.request.ChaosResponse;
import xyz.vopen.framework.chaos.remoting.aio.processor.AbstractMessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

/**
 * {@link ServerMessageProcessor}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/14
 */
public class ServerMessageProcessor extends AbstractMessageProcessor<Request>
    implements ChaosServerGroup {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerMessageProcessor.class);

  private ChaosContext context;

  public ServerMessageProcessor(ChaosContext context) {
    this.context = context;
    new EventThread(context).start();
  }

  @Override
  public void process0(Session session, Request request) {
    if (request instanceof ChaosRequest) {
      this.context.getHandler().handler(session, request, context);
    } else {
      ChaosResponse response = (ChaosResponse) request;
      if (response.isWatch()) {
        // need trigger event
        this.context.getHandler().handler(session, response, context);
      }
    }
  }

  @Override
  public void stateEvent0(Session session, StateMachineEnum stateMachineEnum, Throwable throwable) {
    switch (stateMachineEnum) {
      case NEW_SESSION:
        break;
      case SESSION_CLOSED:
        remove(session);
        LOGGER.info(
            "Chaos server closed , session-id : {} remove sessionMaps", session.getSessionId());
    }
  }

  @Override
  public void destroy() {
    this.context.getEventManager().destroy();
  }

  @Override
  public final void join(Session session) {
    this.context.getEventManager().addServiceNode(session);
  }

  @Override
  public void remove(Session session) {
    this.context.getEventManager().addUnreachableNode(session);
  }

  @Override
  public void writeToGroup(String group, byte[] data) {}
}

interface ChaosServerGroup {

  void join(Session session);

  void remove(Session session);

  void writeToGroup(String group, byte[] data);
}
