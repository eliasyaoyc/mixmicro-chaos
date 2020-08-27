package xyz.vopen.framework.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import xyz.vopen.framework.chaos.client.ChaosClient;

/**
 * {@link ChaosBootstrap}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
@SpringBootApplication
public class ChaosBootstrap {
  public static void main(String[] args) {
    SpringApplication.run(ChaosBootstrap.class, args);
  }
}
