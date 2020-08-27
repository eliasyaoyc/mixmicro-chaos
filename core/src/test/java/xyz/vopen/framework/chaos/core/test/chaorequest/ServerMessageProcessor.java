//package xyz.vopen.framework.chaos.core.test.chaorequest;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import xyz.vopen.framework.chaos.common.exception.ChaosException;
//import xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager;
//import xyz.vopen.framework.chaos.core.processor.chain.Handler;
//import xyz.vopen.framework.chaos.core.processor.chain.HandlerRegister;
//import xyz.vopen.framework.chaos.core.request.ChaosRequest;
//import xyz.vopen.framework.chaos.core.request.ChaosResponse;
//import xyz.vopen.framework.chaos.remoting.aio.processor.AbstractMessageProcessor;
//import xyz.vopen.framework.chaos.remoting.aio.transport.WriterBuffer;
//import xyz.vopen.framework.chaos.remoting.api.Session;
//import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;
//
//import java.io.IOException;
//import java.io.ObjectInput;
//import java.io.Serializable;
//
//import static xyz.vopen.framework.chaos.core.request.AbstractChaosRequest.Type.*;
//
///**
// * {@link ServerMessageProcessor}
// *
// * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
// * @version ${project.version}
// * @date 2020/7/14
// */
//public class ServerMessageProcessor extends AbstractMessageProcessor<byte[]>
//    implements Serializable {
//
//  private static final Logger LOGGER = LoggerFactory.getLogger(ServerMessageProcessor.class);
//
//  private Handler handlerChain;
//  private ChaosEventManager manager;
//  private final Hessian hessian;
//
//  public ServerMessageProcessor() {
//    this.handlerChain = HandlerRegister.INSTANCE.getHandler();
//    if (handlerChain == null) {
//      throw new ChaosException("Handler chain assembly failure.");
//    }
//    this.manager = ChaosEventManager.getInstance();
//    this.hessian = Hessian.getInstance();
//  }
//
//  @Override
//  public void process0(Session<byte[]> session, byte[] request) {
//    ObjectInput objectInput = null;
//    try {
//      ChaosRequest resp = (ChaosRequest) hessian.deserializer(request);
////      objectInput = new ObjectInputStream(new ByteArrayInputStream(request));
////      ChaosRequest resp = (ChaosRequest) objectInput.readObject();
//      LOGGER.info("接收到客户端响应： {}", resp.getRequestType());
//      // validate request type.
//      validateChaosRequest(resp);
//      LOGGER.info(
//          "Received the Chaos request, session-id : {} , request-type : {}",
//          session.getSessionId(),
//          resp.getRequestType());
//
//      // handler chain.
//      ChaosResponse chaosResponse = null;
//      if (chaosResponse == null) {
//        chaosResponse = ChaosResponse.builder().build();
//      }
//      WriterBuffer writeBuffer = (WriterBuffer) session.writeBuffer();
//      //      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//      //      ObjectOutput objectOutput = new ObjectOutputStream(byteArrayOutputStream);
//      //      objectOutput.writeObject(chaosResponse);
//      //      byte[] data = byteArrayOutputStream.toByteArray();
//      byte[] data = (byte[]) hessian.serializer(chaosResponse);
//      try {
//        writeBuffer.writeInt(data.length + 4);
//        writeBuffer.write(data);
//        writeBuffer.flush();
//      } catch (IOException e) {
//        throw new ChaosException("network error : {}", e);
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//
//  @Override
//  public void stateEvent0(
//      Session<byte[]> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
//    switch (stateMachineEnum) {
//      case NEW_SESSION:
//        LOGGER.info("new connection, session-id : {}", session.getSessionId());
//      case SESSION_CLOSED:
//        LOGGER.info("closed , session-id : {}", session.getSessionId());
//      case SESSION_CLOSING:
//        LOGGER.info("closing, session-id : {}", session.getSessionId());
//    }
//  }
//
//  /** Validate chaosRequest whether inner method. */
//  private boolean validateChaosRequest(ChaosRequest request) {
//    if (request.getRequestType() == HEARTBEAT
//        || request.getRequestType() == OMINOUS
//        || request.getRequestType() == ONLINE
//        || request.getRequestType() == OFFLINE
//        || request.getRequestType() == SYNC) {
//      return true;
//    }
//    throw new IllegalArgumentException("Illegal request.");
//  }
//
//  @Override
//  public void destroy() {
//    this.manager.destroy();
//  }
//}
