//package xyz.vopen.framework.chaos.core.test.chaorequest;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import xyz.vopen.framework.chaos.core.request.ChaosRequest;
//import xyz.vopen.framework.chaos.core.request.ChaosResponse;
//import xyz.vopen.framework.chaos.core.test.rpc.rpc.RpcResponse;
//import xyz.vopen.framework.chaos.remoting.aio.processor.AbstractMessageProcessor;
//import xyz.vopen.framework.chaos.remoting.api.Session;
//import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.ObjectInput;
//import java.io.ObjectInputStream;
//import java.io.Serializable;
//
///**
// * {@link ClientMessageProcessor}
// *
// * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
// * @version ${project.version}
// * @date 2020/7/11
// */
//public class ClientMessageProcessor extends AbstractMessageProcessor<byte[]>
//    implements Serializable {
//
//  private static final Logger LOGGER = LoggerFactory.getLogger(ClientMessageProcessor.class);
//
//  public ClientMessageProcessor() {}
//
//  @Override
//  public void process0(Session<byte[]> session, byte[] msg) {
//    ObjectInput objectInput = null;
//    try {
//      ChaosResponse resp = (ChaosResponse) Hessian.getInstance().deserializer(msg);
////      objectInput = new ObjectInputStream(new ByteArrayInputStream(msg));
////      ChaosResponse resp = (ChaosResponse) objectInput.readObject();
//      LOGGER.info("接收到服务端响应： {}", resp.getRequestType());
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//
//  @Override
//  public void stateEvent0(
//      Session<byte[]> session, StateMachineEnum stateMachineEnum, Throwable throwable) {}
//
//  @Override
//  public void destroy() {}
//}
