package xyz.vopen.framework.chaos.client.test;

import afu.org.checkerframework.checker.oigj.qual.O;
import xyz.vopen.framework.chaos.client.ChaosClient;
import xyz.vopen.framework.chaos.client.ChaosClientConfig;
import xyz.vopen.framework.chaos.core.internals.Serializer;
import xyz.vopen.framework.chaos.core.internals.event.ChaosWatcher;

import java.util.concurrent.TimeUnit;

/**
 * {@link ChaosClientTest01}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/26
 */
public class ChaosClientTest01 {

  public static void main(String[] args) {

    ChaosClientConfig config =
        ChaosClientConfig.builder()
            .serializerType(Serializer.SerializerType.JSON.getType())
            .clientId("test-01")
            .registerName("Test")
            .services("192.168.34.224:8887,192.168.34.224:8888")
            .build();

    ChaosClient chaosClient = new ChaosClient(config);

    chaosClient.register(
        new ChaosWatcher() {

          @Override
          public void initializer(String cursor, Object obj) {
            System.out.println("准备好了");
            chaosClient.ack(cursor);
          }

          @Override
          public void started() {
            System.out.println("开始");
            try {
              TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            chaosClient.failure("qw1321");
          }

          @Override
          public void exit() {
            System.out.println("退出");
          }
        });
  }
}
