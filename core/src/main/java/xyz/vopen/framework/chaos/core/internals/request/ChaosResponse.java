package xyz.vopen.framework.chaos.core.internals.request;

import xyz.vopen.framework.chaos.remoting.api.Request;

import java.io.Serializable;
import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type;

/**
 * {@link ChaosResponse}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/13
 */
public class ChaosResponse implements Request, Serializable {

  /** version number. */
  private String version;

  /** request id. */
  private String requestId;

  /** the type of request {@link Type}. */
  private Type requestType;

  /** the type of serializer. */
  private int serializerType;

  /** client id (host + port). */
  private String clientId;

  /** the name of service. */
  private String serviceName;

  /** this request received time, unit is milliseconds. */
  private long receivedTimeMs;

  /** this request create time, unit is milliseconds. */
  private long createTimeMs;

  /** this request timeout time, unit is milliseconds. */
  private long requestTimeoutMs;

  /**
   * The Argument of object that used for record when send exit command and the instance passed to
   * the same serviceName.
   */
  private Object obj;

  private boolean watch;

  public ChaosResponse() {}

  public ChaosResponse(
      String version,
      String requestId,
      Type requestType,
      int serializerType,
      String serviceName,
      String clientId,
      long receivedTimeMs,
      long createTimeMs,
      long requestTimeoutMs,
      Object obj,
      boolean watch) {
    this.version = version;
    this.requestId = requestId;
    this.serializerType = serializerType;
    this.serviceName = serviceName;
    this.requestType = requestType;
    this.clientId = clientId;
    this.receivedTimeMs = receivedTimeMs;
    this.createTimeMs = createTimeMs;
    this.requestTimeoutMs = requestTimeoutMs;
    this.obj = obj;
    this.watch = watch;
  }

  public void setSerializerType(int serializerType) {
    this.serializerType = serializerType;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public Type getRequestType() {
    return requestType;
  }

  public void setRequestType(Type requestType) {
    this.requestType = requestType;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public long getReceivedTimeMs() {
    return receivedTimeMs;
  }

  public void setReceivedTimeMs(long receivedTimeMs) {
    this.receivedTimeMs = receivedTimeMs;
  }

  public long getCreateTimeMs() {
    return createTimeMs;
  }

  public void setCreateTimeMs(long createTimeMs) {
    this.createTimeMs = createTimeMs;
  }

  public long getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  public void setRequestTimeoutMs(long requestTimeoutMs) {
    this.requestTimeoutMs = requestTimeoutMs;
  }

  public boolean isWatch() {
    return watch;
  }

  public void setWatch(boolean watch) {
    this.watch = watch;
  }

  public static ChaosResponseBuilder builder() {
    return new ChaosResponseBuilder();
  }

  @Override
  public int getType() {
    return this.requestType.getType();
  }

  @Override
  public int getSerializerType() {
    return this.serializerType;
  }

  public Object getObj() {
    if (obj == null) {
      return new Object();
    }
    return this.obj;
  }

  public void setObj(Object obj) {
    this.obj = obj;
  }

  public static class ChaosResponseBuilder {
    private String version;
    private String requestId;
    private Type requestType;
    private int serializerType;
    private String serviceName;
    private String clientId;
    private long receivedTimeMs;
    private long createTimeMs;
    private long requestTimeoutMs;
    private Object obj;
    private boolean watch;

    public ChaosResponseBuilder() {}

    public ChaosResponse build() {
      return new ChaosResponse(
          version,
          requestId,
          requestType,
          serializerType,
          serviceName,
          clientId,
          receivedTimeMs,
          createTimeMs,
          requestTimeoutMs,
          obj,
          watch);
    }

    public ChaosResponseBuilder serializerType(int serializerType) {
      this.serializerType = serializerType;
      return this;
    }

    public ChaosResponseBuilder version(String version) {
      this.version = version;
      return this;
    }

    public ChaosResponseBuilder requestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public ChaosResponseBuilder requestType(Type requestType) {
      this.requestType = requestType;
      return this;
    }

    public ChaosResponseBuilder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public ChaosResponseBuilder sendTimeMs(long receivedTimeMs) {
      this.receivedTimeMs = receivedTimeMs;
      return this;
    }

    public ChaosResponseBuilder createTimeMs(long createTimeMs) {
      this.createTimeMs = createTimeMs;
      return this;
    }

    public ChaosResponseBuilder requestTimeout(long requestTimeoutMs) {
      this.requestTimeoutMs = requestTimeoutMs;
      return this;
    }

    public ChaosResponseBuilder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public ChaosResponseBuilder obj(Object obj) {
      this.obj = obj;
      return this;
    }

    public ChaosResponseBuilder watch(boolean watch) {
      this.watch = watch;
      return this;
    }
  }
}
