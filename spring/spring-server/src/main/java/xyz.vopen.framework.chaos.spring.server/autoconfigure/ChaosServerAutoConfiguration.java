package xyz.vopen.framework.chaos.spring.server.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import xyz.vopen.framework.chaos.client.ChaosClient;
import xyz.vopen.framework.chaos.client.ChaosClientConfig;
import xyz.vopen.framework.chaos.core.ChaosCoordinator;
import xyz.vopen.framework.chaos.core.internals.ChaosServerContext;
import xyz.vopen.framework.chaos.core.internals.ChaosThread;
import xyz.vopen.framework.chaos.core.internals.EventManager;
import xyz.vopen.framework.chaos.core.internals.Serializer;
import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager;
import xyz.vopen.framework.chaos.remoting.aio.ChaosServerConfig;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.spring.server.autoconfigure.ChaosServerProperties;

import javax.validation.constraints.NotNull;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReentrantLock;

import static xyz.vopen.framework.chaos.spring.server.autoconfigure.ChaosServerProperties.CHAOS_SERVER_PROPERTIES_PREFIX;
import static xyz.vopen.framework.chaos.spring.server.autoconfigure.ChaosServerProperties.CHAOS_SERVER_PROPERTIES_PREFIX;

/**
 * {@link ChaosServerAutoConfiguration}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
@ConditionalOnProperty(
    prefix = CHAOS_SERVER_PROPERTIES_PREFIX,
    value = "enabled",
    havingValue = "true")
@Configuration
@EnableConfigurationProperties(ChaosServerProperties.class)
public class ChaosServerAutoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChaosServerAutoConfiguration.class);

  private static final int DEFAULT_PORT = 8888;

  @Value("${spring.application.name}")
  private String serviceName;

  @Bean
  ApplicationReadyEventListener applicationReadyEventListener(
      ChaosServerProperties chaosServerProperties) {
    chaosServerProperties.setServiceName(serviceName);
    return new ApplicationReadyEventListener(chaosServerProperties);
  }

  static class ApplicationReadyEventListener
      implements ApplicationListener<SpringApplicationEvent> {
    private final ChaosServerProperties chaosServerProperties;

    public ApplicationReadyEventListener(ChaosServerProperties chaosServerProperties) {
      this.chaosServerProperties = chaosServerProperties;
    }

    /**
     * Handle an application event.
     *
     * @param springApplicationEvent the event to respond to.
     */
    @Override
    public void onApplicationEvent(@NotNull SpringApplicationEvent springApplicationEvent) {
      if (springApplicationEvent instanceof ApplicationReadyEvent) {
        LOGGER.info(" Application Context is ready, staring chaos coordinator runner....");
        ChaosServerConfig chaosServerConfig = new ChaosServerConfig();
        setConfig(chaosServerProperties, chaosServerConfig);

        // first start Coordinator.
        ChaosCoordinator coordinator = new ChaosCoordinator(chaosServerConfig);
        coordinator.preInit();
        coordinator.start();
        ChaosServerContext context = coordinator.getContext();
        EventManager eventManager = context.getEventManager();

        // second start chaos client.
        ChaosClientConfig chaosClientConfig =
            ChaosClientConfig.builder()
                .serializerType(chaosServerConfig.getSerializerType())
                .clientId(chaosServerConfig.getClientId())
                .registerName("coordinator")
                .services(this.chaosServerProperties.getServices())
                .build();

        chaosClientConfig.setPort(chaosClientConfig.getPort());
        chaosClientConfig.setCoordinator(true);
        ChaosClient client = new ChaosClient(chaosClientConfig);
        client.getContext().setEventManager(eventManager);
        client.startCoordinatorClient();

        //        new SyncThead(context, client, chaosClientConfig).start();
      }
    }

    private void setConfig(ChaosServerProperties properties, ChaosServerConfig config) {
      try {
        InetAddress localHost = Inet4Address.getLocalHost();
        String hostAddress = localHost.getHostAddress();
        BeanUtils.copyProperties(properties, config);
        config.setHost(hostAddress);
        if (properties.getSerializerType() != null) {
          switch (properties.getSerializerType()) {
            case "json":
              config.setSerializerType(Serializer.SerializerType.JSON.getType());
              break;
            case "hessian":
              config.setSerializerType(Serializer.SerializerType.HESSIAN.getType());
          }
        } else {
          config.setSerializerType(Serializer.SerializerType.HESSIAN.getType());
        }
        config.setCoordinator(true);

        // configure coordinator port.
        String port = System.getProperty("serverPort");
        if (StringUtils.isEmpty(port)) {
          config.setPort(DEFAULT_PORT);
        } else {
          config.setPort(Integer.parseInt(port));
        }

        // configure log dir.
        String logDir = System.getProperty("logDir");
        if (!StringUtils.isEmpty(logDir)) {
          config.setLogDir(logDir);
        }
        //        config.setServiceName("coordinator");
      } catch (UnknownHostException e) {
        e.printStackTrace();
      }
    }

    class SyncThead extends ChaosThread {

      private static final String EVENT_THREAD_NAME = "SyncThread";

      private final ReentrantLock lock = new ReentrantLock();

      private final ChaosContext context;
      private final ChaosClient chaosClient;
      private final ChaosClientConfig config;
      private volatile boolean running = false;

      public SyncThead(ChaosContext context, ChaosClient chaosClient, ChaosClientConfig config) {
        super(EVENT_THREAD_NAME);
        this.context = context;
        this.chaosClient = chaosClient;
        this.config = config;
        this.running = true;
      }

      @Override
      public void run() {
        try {
          while (running) {
            ChaosEventManager.ChaosRequestPair pair =
                this.context.getEventManager().getResponse(true);
            if (pair != null) {
              Request request = pair.getRequest();
              request.setClientId(config.getClientId());
              chaosClient.send(pair.getRequest());
            }
          }
        } catch (Exception e) {
          LOGGER.error("Event thread exiting due to interrupted", e);
          this.running = false;
        }
      }
    }
  }
}
