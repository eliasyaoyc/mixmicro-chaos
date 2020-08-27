//package xyz.vopen.framework.chaos.core.test.chaorequest;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import xyz.vopen.framework.chaos.core.Chaos;
//import xyz.vopen.framework.chaos.core.ChaosConfig;
//import xyz.vopen.framework.chaos.core.test.rpc.rpc.RpcProtocol;
//import xyz.vopen.framework.chaos.remoting.aio.ChaosServer;
//import xyz.vopen.framework.chaos.remoting.aio.ChaosServerConfig;
//
///**
// * {@link ChaosCoordinator} This is a main class.
// *
// * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
// * @version ${project.version}
// * @date 2020/7/14
// */
//public class ChaosCoordinator {
//
//  private static final Logger LOGGER = LoggerFactory.getLogger(ChaosCoordinator.class);
//
//  private ChaosConfig config;
//  private Chaos chaosClient;
//  private ChaosServer chaosServer;
//
//  public ChaosCoordinator(ChaosConfig config) {
//    this.config = config;
//    this.config.getChaosServerConfig().setProcessor(new ServerMessageProcessor());
//    this.config
//        .getChaosServerConfig()
//        .setProtocol(new RpcProtocol());
//    this.chaosServer = new ChaosServer(this.config.getChaosServerConfig());
//    this.chaosClient = new Chaos(this.config.getChaosServerConfig());
//  }
//
//  public static void main(String[] args) {
//    try {
//      // assembly config.
//      ChaosConfig config = new ChaosConfig(args);
//
//      // construct chaos object.
//      final ChaosCoordinator chaos = new ChaosCoordinator(config);
//
//      // additional shutdown hook.
//      Runtime.getRuntime().addShutdownHook(new Thread(chaos::shutdown));
//
//      // start.
//      chaos.start();
//    } catch (Exception e) {
//      e.printStackTrace();
//      System.exit(1);
//    }
//  }
//
//  private void start() {
//    try {
//      // start chaos server.
//      this.chaosServer.start();
//      //      this.chaosClient.start();
//    } catch (Exception e) {
//      ChaosServerConfig chaosServerConfig = this.config.getChaosServerConfig();
//      LOGGER.error(
//          "Coordinator start occur failure , host : {}, port: : {}",
//          this.config.getChaosServerConfig().getHost(),
//          this.config.getChaosServerConfig().getPort());
//      shutdown();
//    }
//  }
//
//  private void shutdown() {
//    try {
//      this.chaosClient.destroy();
//      this.chaosServer.destroy();
//      this.config.getChaosServerConfig().getProcessor().destroy();
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//}
