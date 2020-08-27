package xyz.vopen.framework.chaos.client;

import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.remoting.aio.AbstractChaosConfig;
import xyz.vopen.framework.chaos.remoting.aio.ChaosServerConfig;

import java.util.UUID;

/**
 * {@link ChaosClientConfig} A configuration for create a {@link
 * xyz.vopen.framework.chaos.client.ChaosClient}.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/25
 */
public class ChaosClientConfig extends ChaosServerConfig {

  /** Chaos server cluster address (must not empty). */
  private String services;

  /** The name of service that is guaranteed to Ha (must not empty). */
  private String registerName;

  /** The id of service. it will be randomly generated if it is not set. */
  private String clientId = UUID.randomUUID().toString();

  /** The type of serializer default is json. */
  private int serializerType = 1;

  /** The interval heartbeat plugin. */
  private long heartbeatInterval = 10000;

  private long requestTimeout = 10000;

  private int port;

  /**
   * whether coordinator.
   */
  private boolean isCoordinator = false;

  private ChaosClientConfig(
      String services,
      String registerName,
      String clientId,
      int serializerType,
      long heartbeatInterval,
      long requestTimeout,
      int port) {

    this.services = services;
    this.registerName = registerName;
    this.clientId = clientId;
    this.serializerType = serializerType;
    this.heartbeatInterval = heartbeatInterval;
    this.requestTimeout = requestTimeout;
    this.port = port;
  }

  public String getServices() {
    return services;
  }

  public void setServices(String services) {
    this.services = services;
  }

  public String getRegisterName() {
    return registerName;
  }

  public void setRegisterName(String registerName) {
    this.registerName = registerName;
  }

  public String getClientId() {
    return clientId;
  }

  @Override
  public String getServiceName() {
    return this.registerName;
  }

  @Override
  public long getRequestTimeout() {
    return this.requestTimeout;
  }

  public void setRequestTimeout(long requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  @Override
  public boolean isCoordinator() {
    return this.isCoordinator;
  }

  public void setCoordinator(boolean coordinator) {
    isCoordinator = coordinator;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public int getSerializerType() {
    return serializerType;
  }

  public void setSerializerType(int serializerType) {
    this.serializerType = serializerType;
  }

  public long getHeartbeatInterval() {
    return heartbeatInterval;
  }

  public void setHeartbeatInterval(long heartbeatInterval) {
    this.heartbeatInterval = heartbeatInterval;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void setPort(int port) {
    this.port = port;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String services;

    private String registerName;

    private String clientId;

    private int serializerType;

    private long heartbeatInterval;

    private long requestTimeout;

    private int port;

    public Builder services(String services) {
      this.services = services;
      return this;
    }

    public Builder registerName(String registerName) {
      this.registerName = registerName;
      return this;
    }

    public Builder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder serializerType(int serializerType) {
      this.serializerType = serializerType;
      return this;
    }

    public ChaosClientConfig build() {
      return new ChaosClientConfig(
          services, registerName, clientId, serializerType, heartbeatInterval,requestTimeout,port);
    }
  }
}
