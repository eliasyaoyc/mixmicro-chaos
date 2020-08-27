package xyz.vopen.framework.chaos.client.test;

import xyz.vopen.framework.chaos.client.ChaosClient;
import xyz.vopen.framework.chaos.client.ChaosClientConfig;
import xyz.vopen.framework.chaos.core.internals.Serializer;
import xyz.vopen.framework.chaos.core.internals.event.ChaosWatcher;

/**
 * {@link ChaosClientTest02}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/26
 */
public class ChaosClientTest02 {

  public static void main(String[] args) {

    ChaosClientConfig config =
        ChaosClientConfig.builder()
            .serializerType(Serializer.SerializerType.JSON.getType())
            .clientId("test-02")
            .registerName("Test")
            .services("192.168.34.224:8887,192.168.34.224:8888")
            .build();

    ChaosClient chaosClient = new ChaosClient(config);

    chaosClient.register(
        new ChaosWatcher() {
          @Override
          public void initializer(String cursor, Object obj) {
            System.out.println("准备好了");
            System.out.println(obj == null ? "没有" : obj.toString());
            chaosClient.ack(cursor);
          }

          @Override
          public void started() {
            System.out.println("开始");
          }

          @Override
          public void exit() {
            System.out.println("退出");
          }
        });
  }
}
