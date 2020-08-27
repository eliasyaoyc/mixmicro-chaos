package xyz.vopen.framework.chaos.remoting.aio;

import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferFactory;
import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.Protocol;

import java.net.SocketOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@link ChaosServerConfig}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class ChaosServerConfig extends AbstractChaosConfig {

  /**
   * The name of service.
   */
  private String serviceName;

  private String clientId = UUID.randomUUID().toString();

  /**
   * The maximum per connection count.
   */
  private int maxInFlightRequestsPerConnection = 100;

  /**
   * Chaos server version.
   */
  private String chaosVersion;

  /** Chaos client direct connect address. */
  private String destinations;

  /** The address that the cluster of coordinator node. */
  private String services;

  /** The requestBody buffer byte. */
  private int readBufferSize = 512;

  /** The limit of memory block. */
  private int writeBufferSize = 512;

  /** The capacity of write buffer */
  private int writeBufferCapacity = 512;

  /** backlog */
  private int backlog = 1000;

  /**
   * The type of serialization.
   */
  private int serializerType;

  /**
   * whether coordinator.
   */
  private boolean isCoordinator = true;

  /**
   * The time that request timeout.
   */
  private long requestTimeout = 2000;

  private long heartbeatInterval = 5000;
  /**
   * message processor {@link
   * xyz.vopen.framework.chaos.remoting.aio.processor.AbstractMessageProcessor}
   */
  private MessageProcessor processor;

  /**
   * Chaos protocol.
   */
  private Protocol protocol;

  /** socket configuration */
  private Map<SocketOption<Object>, Object> socketOptions;

  /** the number of thread */
  private int threadNum = 2;

  /** memory pool */
  private BufferFactory bufferFactory = BufferFactory.DISABLED_BUFFER_FACTORY;

  private String host;

  private int port;

  /**
   * The location of log store.;
   */
  private String logDir;


  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getLogDir() {
    return logDir;
  }

  public void setLogDir(String logDir) {
    this.logDir = logDir;
  }

  public boolean isCoordinator() {
    return isCoordinator;
  }

  public void setCoordinator(boolean coordinator) {
    isCoordinator = coordinator;
  }

  public long getHeartbeatInterval() {
    return heartbeatInterval;
  }

  public void setHeartbeatInterval(long heartbeatInterval) {
    this.heartbeatInterval = heartbeatInterval;
  }

  public int getSerializerType() {
    return serializerType;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setSerializerType(int serializerType) {
    this.serializerType = serializerType;
  }

  public void setSocketOptions(Map<SocketOption<Object>, Object> socketOptions) {
    this.socketOptions = socketOptions;
  }

  public String getDestinations() {
    return destinations;
  }

  public void setDestinations(String destinations) {
    this.destinations = destinations;
  }

  public long getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(long requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public String getChaosVersion() {
    return chaosVersion;
  }

  public void setChaosVersion(String chaosVersion) {
    this.chaosVersion = chaosVersion;
  }

  @Override
  public String getServices() {
    return services;
  }

  public void setServices(String services) {
    this.services = services;
  }

  public int getMaxInFlightRequestsPerConnection() {
    return maxInFlightRequestsPerConnection;
  }

  public void setMaxInFlightRequestsPerConnection(int maxInFlightRequestsPerConnection) {
    this.maxInFlightRequestsPerConnection = maxInFlightRequestsPerConnection;
  }


  /** Return the default of memory buffer. */
  public int getWriteBufferSize() {
    return writeBufferSize;
  }

  public void setWriteBufferSize(int writeBufferSize) {
    this.writeBufferSize = writeBufferSize;
  }


  public Protocol getProtocol() {
    return this.protocol;
  }

  public void setProtocol(Protocol protocol) {
    this.protocol = protocol;
  }


  public MessageProcessor getProcessor() {
    return processor;
  }

  public void setProcessor(MessageProcessor processor) {
    this.processor = processor;
  }

  public int getReadBufferSize() {
    return readBufferSize;
  }

  public void setReadBufferSize(int readBufferSize) {
    this.readBufferSize = readBufferSize;
  }

  public Map<SocketOption<Object>, Object> getSocketOptions() {
    return socketOptions;
  }

  public void setOption(SocketOption socketOption, Object f) {
    if (socketOptions == null) {
      socketOptions = new HashMap<>(4);
    }
    socketOptions.put(socketOption, f);
  }

  public int getWriteBufferCapacity() {
    return writeBufferCapacity;
  }

  public void setWriteBufferCapacity(int writeBufferCapacity) {
    this.writeBufferCapacity = writeBufferCapacity;
  }

  public int getThreadNum() {
    return threadNum;
  }

  public void setThreadNum(int threadNum) {
    this.threadNum = threadNum;
  }

  public BufferFactory getBufferFactory() {
    return bufferFactory;
  }

  public void setBufferFactory(BufferFactory bufferFactory) {
    this.bufferFactory = bufferFactory;
  }

  public int getBacklog() {
    return backlog;
  }

  public void setBacklog(int backlog) {
    this.backlog = backlog;
  }
}
