package xyz.vopen.framework.chaos.spring.client.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import xyz.vopen.framework.chaos.client.ChaosClient;
import xyz.vopen.framework.chaos.client.ChaosClientConfig;
import xyz.vopen.framework.chaos.client.event.ExitEvent;
import xyz.vopen.framework.chaos.client.event.InitializerEvent;
import xyz.vopen.framework.chaos.client.event.StartedEvent;
import xyz.vopen.framework.chaos.core.internals.Serializer;
import xyz.vopen.framework.chaos.core.internals.event.ChaosWatcher;

import static xyz.vopen.framework.chaos.spring.client.autoconfigure.ChaosClientProperties.CHAOS_CLIENT_PROPERTIES_PREFIX;

/**
 * {@link ChaosClientAutoConfiguration}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/8/1
 */
@ConditionalOnProperty(
    prefix = CHAOS_CLIENT_PROPERTIES_PREFIX,
    value = "enabled",
    havingValue = "true")
@Configuration
@EnableConfigurationProperties(ChaosClientProperties.class)
public class ChaosClientAutoConfiguration implements ApplicationContextAware {

  private ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.context = applicationContext;
  }

  @Bean
  ChaosClient chaosClient(ChaosClientProperties properties) {
    if (StringUtils.isEmpty(properties.getServices())
        || StringUtils.isEmpty(properties.getRegisterName())) {
      throw new IllegalArgumentException(
          "Argument [ Services or RegisterName ] must not be empty.");
    }
    ChaosClientConfig config =
        ChaosClientConfig.builder()
            .serializerType(
                properties.getSerializerType().equals("json")
                    ? Serializer.SerializerType.JSON.getType()
                    : Serializer.SerializerType.HESSIAN.getType())
            .clientId(properties.getClientId())
            .registerName(properties.getRegisterName())
            .services(properties.getServices())
            .build();
    ChaosClient chaosClient = new ChaosClient(config);

    chaosClient.register(
        new ChaosWatcher() {

          @Override
          public void initializer(String cursor, Object obj) {
            context.publishEvent(
                new InitializerEvent(new InitializerEvent.InitializerEventPair(cursor, obj)));
          }

          @Override
          public void started() {
            context.publishEvent(new StartedEvent(null));
          }

          @Override
          public void exit() {
            context.publishEvent(new ExitEvent(null));
          }
        });
    return chaosClient;
  }
}
