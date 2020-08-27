//package xyz.vopen.framework.chaos.core.test.heartbeat;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import xyz.vopen.framework.chaos.remoting.aio.ChaosNetwork;
//import xyz.vopen.framework.chaos.remoting.aio.processor.AbstractMessageProcessor;
//import xyz.vopen.framework.chaos.remoting.aio.transport.WriterBuffer;
//import xyz.vopen.framework.chaos.remoting.api.Session;
//import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;
//
//import java.io.IOException;
//import java.util.concurrent.ExecutionException;
//
///**
// * {@link TestClient}
// *
// * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
// * @version ${project.version}
// * @date 2020/7/16
// */
//public class TestClient {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class);
//
//
//    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
//        AbstractMessageProcessor<String> client_1_processor = new AbstractMessageProcessor<String>() {
//            @Override
//            public void destroy() {
//
//            }
//
//            @Override
//            public void process0(Session<String> session, String msg) {
//                LOGGER.info("client_1 收到服务端消息:" + msg);
//            }
//
//            @Override
//            public void stateEvent0(Session<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
//                LOGGER.info("stateMachineEnum：{}", stateMachineEnum);
//            }
//        };
//        ChaosNetwork<String> client_1 = new ChaosNetwork<String>("localhost", 8888, new StringProtocol(), client_1_processor);
//        client_1.start();
//
//        AbstractMessageProcessor<String> client_2_processor = new AbstractMessageProcessor<String>() {
//            @Override
//            public void destroy() {
//
//            }
//
//            @Override
//            public void process0(Session<String> session, String msg) {
//                LOGGER.info("client_2 收到服务端消息:" + msg);
//                try {
//                    if ("heart_req".equals(msg)) {
//                        WriterBuffer writerBuffer = (WriterBuffer) session.writeBuffer();
//                        byte[] heartBytes = "heart_rsp".getBytes();
//                        writerBuffer.writeInt(heartBytes.length);
//                        writerBuffer.write(heartBytes);
//                        LOGGER.info("client_2 发送心跳响应消息");
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void stateEvent0(Session<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
//                LOGGER.info("stateMachineEnum：{}", stateMachineEnum);
//            }
//        };
//        ChaosNetwork<String> client_2 = new ChaosNetwork<String>("localhost", 8888, new StringProtocol(), client_2_processor);
//        client_2.start();
//    }
//}
