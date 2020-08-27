package xyz.vopen.framework.chaos.spring.server.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.SocketOption;
import java.util.Map;

/**
 * {@link ChaosServerProperties}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
@ConfigurationProperties(prefix = ChaosServerProperties.CHAOS_SERVER_PROPERTIES_PREFIX)
public class ChaosServerProperties<T> {

  public static final String CHAOS_SERVER_PROPERTIES_PREFIX = "mixmicro.chaos";

  private String serviceName;

  /** The RequestBody buffer size. */
  private int readBufferSize;

  /** The limit of memory block. */
  private int writeBufferSize;

  /** The capacity of writer buffer. */
  private int writeBufferCapacity;

  private String host;

  /** Backlog */
  private int backlog;

  private Map<SocketOption<Object>, Object> socketOptions;

  private int threadNum;

  /** The type of serialization. */
  private String serializerType;

  /** The maximum per connection count. */
  private int maxInFlightRequestsPerConnection;

  /** The time that request timeout. */
  private long requestTimeout;

  /** The interval of heartbeat. */
  private long heartbeatInterval;

  /** The address that the cluster of coordinator node. */
  private String services;

  /** Chaos client direct connect address. */
  private String destinations;

  public long getHeartbeatInterval() {
    return heartbeatInterval;
  }

  public void setHeartbeatInterval(long heartbeatInterval) {
    this.heartbeatInterval = heartbeatInterval;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getServices() {
    return services;
  }

  public void setServices(String services) {
    this.services = services;
  }

  public String getDestinations() {
    return destinations;
  }

  public void setDestinations(String destinations) {
    this.destinations = destinations;
  }

  public String getSerializerType() {
    return serializerType;
  }

  public void setSerializerType(String serializerType) {
    this.serializerType = serializerType;
  }

  public int getMaxInFlightRequestsPerConnection() {
    return maxInFlightRequestsPerConnection;
  }

  public void setMaxInFlightRequestsPerConnection(int maxInFlightRequestsPerConnection) {
    this.maxInFlightRequestsPerConnection = maxInFlightRequestsPerConnection;
  }

  public long getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(long requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public static String getChaosServerPropertiesPrefix() {
    return CHAOS_SERVER_PROPERTIES_PREFIX;
  }

  public int getReadBufferSize() {
    return readBufferSize;
  }

  public void setReadBufferSize(int readBufferSize) {
    this.readBufferSize = readBufferSize;
  }

  public int getWriteBufferSize() {
    return writeBufferSize;
  }

  public void setWriteBufferSize(int writeBufferSize) {
    this.writeBufferSize = writeBufferSize;
  }

  public int getWriteBufferCapacity() {
    return writeBufferCapacity;
  }

  public void setWriteBufferCapacity(int writeBufferCapacity) {
    this.writeBufferCapacity = writeBufferCapacity;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getBacklog() {
    return backlog;
  }

  public void setBacklog(int backlog) {
    this.backlog = backlog;
  }

  public Map<SocketOption<Object>, Object> getSocketOptions() {
    return socketOptions;
  }

  public void setSocketOptions(Map<SocketOption<Object>, Object> socketOptions) {
    this.socketOptions = socketOptions;
  }

  public int getThreadNum() {
    return threadNum;
  }

  public void setThreadNum(int threadNum) {
    this.threadNum = threadNum;
  }
}
