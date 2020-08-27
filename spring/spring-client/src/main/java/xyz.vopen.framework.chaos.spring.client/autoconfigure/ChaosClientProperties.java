package xyz.vopen.framework.chaos.spring.client.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import xyz.vopen.framework.chaos.core.internals.Serializer;

import javax.validation.constraints.NotNull;
import java.util.UUID;

import static xyz.vopen.framework.chaos.spring.client.autoconfigure.ChaosClientProperties.CHAOS_CLIENT_PROPERTIES_PREFIX;

/**
 * {@link ChaosClientProperties}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/8/1
 */
@ConfigurationProperties(prefix = CHAOS_CLIENT_PROPERTIES_PREFIX)
public class ChaosClientProperties {

  public static final String CHAOS_CLIENT_PROPERTIES_PREFIX = "mixmicro.chaos.client";

  private String serializerType = "json";

  private String clientId = UUID.randomUUID().toString();

  private String registerName;

  private String services;

  public @NotNull String getSerializerType() {
    return serializerType;
  }

  public void setSerializerType(String serializerType) {
    this.serializerType = serializerType;
  }

  public @NotNull String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getRegisterName() {
    return registerName;
  }

  public void setRegisterName(String registerName) {
    this.registerName = registerName;
  }

  public String getServices() {
    return services;
  }

  public void setServices(String services) {
    this.services = services;
  }
}
