//package xyz.vopen.framework.chaos.core.test.rpc;
//
//import xyz.vopen.framework.chaos.core.test.rpc.api.DemoApi;
//import xyz.vopen.framework.chaos.core.test.rpc.api.DemoApiImpl;
//import xyz.vopen.framework.chaos.core.test.rpc.rpc.RpcProtocol;
//import xyz.vopen.framework.chaos.core.test.rpc.rpc.RpcProviderProcessor;
//import xyz.vopen.framework.chaos.remoting.aio.ChaosServer;
//
//import java.io.IOException;
//
//public class Provider {
//  public static void main(String[] args) throws IOException {
//    RpcProviderProcessor rpcProviderProcessor = new RpcProviderProcessor();
//    ChaosServer<byte[]> server =
//        new ChaosServer<byte[]>(8888, new RpcProtocol(), rpcProviderProcessor);
//    server.start();
//
//    rpcProviderProcessor.publishService(DemoApi.class, new DemoApiImpl());
//  }
//}
