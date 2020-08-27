//package xyz.vopen.framework.chaos.core.test.heartbeat;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import xyz.vopen.framework.chaos.core.plugins.HeartBeatPlugin;
//import xyz.vopen.framework.chaos.remoting.aio.ChaosServer;
//import xyz.vopen.framework.chaos.remoting.aio.processor.AbstractMessageProcessor;
//import xyz.vopen.framework.chaos.remoting.aio.transport.WriterBuffer;
//import xyz.vopen.framework.chaos.remoting.api.Session;
//import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;
//
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//
///**
// * {@link TestServer}
// *
// * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
// * @version ${project.version}
// * @date 2020/7/16
// */
//public class TestServer {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(TestServer.class);
//
//    public static void main(String[] args) throws IOException {
//        //定义消息处理器
//        AbstractMessageProcessor<String> processor = new AbstractMessageProcessor<String>() {
//            @Override
//            public void destroy() {
//
//            }
//
//            @Override
//            public void process0(Session<String> session, String msg) {
//                LOGGER.info("收到客户端:{}消息：{}", session.getSessionId(), msg);
//            }
//
//            @Override
//            public void stateEvent0(Session<String> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
//                switch (stateMachineEnum) {
//                    case SESSION_CLOSED:
//                        LOGGER.info("客户端:{} 断开连接", session.getSessionId());
//                        break;
//                    case SESSION_CLOSING:
//                        LOGGER.info("客户端:{} 正在断开连接",session.getSessionId());
//                }
//            }
//        };
//
//        //注册心跳插件：每隔1秒发送一次心跳请求，5秒内未收到消息超时关闭连接
//        processor.addPlugin(new HeartBeatPlugin<String>(1, 5, TimeUnit.SECONDS) {
//            @Override
//            public void sendHeartRequest(Session session) throws IOException {
//                WriterBuffer writeBuffer = (WriterBuffer) session.writeBuffer();
//                byte[] heartBytes = "heart_req".getBytes();
//                writeBuffer.writeInt(heartBytes.length);
//                writeBuffer.write(heartBytes);
//                writeBuffer.flush();
//                LOGGER.info("发送心跳请求至客户端:{}", session.getSessionId());
//            }
//
//            @Override
//            public boolean isHeartBeatMessage(Session session, String msg) {
//                //心跳请求消息,返回响应
//                if ("heart_req".equals(msg)) {
//                    try {
//                        WriterBuffer writeBuffer = (WriterBuffer) session.writeBuffer();
//                        byte[] heartBytes = "heart_rsp".getBytes();
//                        writeBuffer.writeInt(heartBytes.length);
//                        writeBuffer.write(heartBytes);
//                        writeBuffer.flush();
//                    } catch (Exception e) {
//                    }
//                    return true;
//                }
//                //是否为心跳响应消息
//                if ("heart_rsp".equals(msg)) {
//                    LOGGER.info("收到来自客户端:{} 的心跳响应消息", session.getSessionId());
//                    return true;
//                }
//                return false;
//            }
//        });
//
//        //启动服务
//        ChaosServer<String> server = new ChaosServer<String>(8888, new StringProtocol(), processor);
//        server.start();
//    }
//}
