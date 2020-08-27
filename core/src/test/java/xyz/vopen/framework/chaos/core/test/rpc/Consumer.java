//package xyz.vopen.framework.chaos.core.test.rpc;
//
//import xyz.vopen.framework.chaos.core.test.rpc.api.DemoApi;
//import xyz.vopen.framework.chaos.core.test.rpc.rpc.RpcConsumerProcessor;
//import xyz.vopen.framework.chaos.core.test.rpc.rpc.RpcProtocol;
//import xyz.vopen.framework.chaos.remoting.aio.ChaosNetwork;
//
//import java.io.IOException;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class Consumer {
//
//  public static void main(String[] args)
//      throws InterruptedException, ExecutionException, IOException {
//
//    RpcConsumerProcessor rpcConsumerProcessor = new RpcConsumerProcessor();
//    ChaosNetwork<byte[]> consumer =
//        new ChaosNetwork<byte[]>("localhost", 8888, new RpcProtocol(), rpcConsumerProcessor);
//    consumer.start();
//
//    DemoApi demoApi = rpcConsumerProcessor.getObject(DemoApi.class);
//    ExecutorService pool = Executors.newCachedThreadPool();
//    pool.execute(
//        () -> {
//          System.out.println(demoApi.test("smart-socket"));
//        });
//    pool.execute(
//        () -> {
//          System.out.println(demoApi.test("smart-socket2"));
//        });
//    pool.execute(
//        () -> {
//          System.out.println(demoApi.sum(1, 2));
//        });
//  }
//}
