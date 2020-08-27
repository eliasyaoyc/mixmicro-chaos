package xyz.vopen.framework.chaos.core.internals.request;

import xyz.vopen.framework.chaos.core.internals.event.ChaosWatcher;
import xyz.vopen.framework.chaos.remoting.api.Request;

import java.io.Serializable;
import java.time.ZonedDateTime;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type;

/**
 * {@link ChaosRequest}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/13
 */
public class ChaosRequest implements Serializable, Request {

  /** version number. */
  private String version;

  /** request id. */
  private String requestId;

  /** the name of service. */
  private String serviceName;

  /** the type of request {@link ChaosRequestFactory.Type}. */
  private Type requestType;

  /**
   * the type of serializer {@link
   * xyz.vopen.framework.chaos.core.internals.Serializer.SerializerType}
   */
  private int serializerType;

  /** client id (host + port). */
  private String clientId;

  /** this request send time, unit is milliseconds. */
  private long sendTimeMs;

  /** this request create time, unit is milliseconds. */
  private long createTimeMs;

  /** this request timeout time, unit is milliseconds. */
  private long requestTimeoutMs;

  /**
   * The Argument of object that used for record when send exit command and the instance passed to
   * the same serviceName.
   */
  private Object obj;

  /** watch */
  private transient ChaosWatcher watcher;

  public ChaosRequest() {}

  public ChaosRequest(
      String version,
      String requestId,
      String serviceName,
      Type requestType,
      int serializerType,
      String clientId,
      String coordinatorId,
      long sendTimeMs,
      long createTimeMs,
      long requestTimeoutMs,
      Object obj,
      ChaosWatcher watcher) {
    this.version = version;
    this.requestId = requestId;
    this.serviceName = serviceName;
    this.requestType = requestType;
    this.serializerType = serializerType;
    this.clientId = clientId;
    this.sendTimeMs = sendTimeMs;
    this.createTimeMs = createTimeMs;
    this.requestTimeoutMs = requestTimeoutMs;
    this.obj = obj;
    this.watcher = watcher;
  }

  public void setSerializerType(int serializerType) {
    this.serializerType = serializerType;
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

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public ChaosRequestFactory.Type getRequestType() {
    return requestType;
  }

  public void setRequestType(ChaosRequestFactory.Type requestType) {
    this.requestType = requestType;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public long getSendTimeMs() {
    return sendTimeMs;
  }

  public void setSendTimeMs(long sendTimeMs) {
    this.sendTimeMs = sendTimeMs;
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

  public ChaosWatcher getWatcher() {
    return watcher;
  }

  public void setWatcher(ChaosWatcher watcher) {
    this.watcher = watcher;
  }

  public static ChaosRequestBuilder builder() {
    return new ChaosRequestBuilder();
  }

  public Object getObj() {
    return obj;
  }

  public void setObj(Object obj) {
    this.obj = obj;
  }

  @Override
  public int getType() {
    return this.requestType.getType();
  }

  @Override
  public int getSerializerType() {
    return this.serializerType;
  }

  public static class ChaosRequestBuilder {
    private String version;
    private String requestId;
    private String serviceName;
    private ChaosRequestFactory.Type requestType;
    private int serializerType;
    private String clientId;
    private String coordinatorId;
    private long sendTimeMs;
    private long createTimeMs;
    private long requestTimeoutMs;
    private Object obj;
    private ChaosWatcher watcher;

    public ChaosRequestBuilder() {}

    public ChaosRequest build() {
      return new ChaosRequest(
          version,
          requestId,
          serviceName,
          requestType,
          serializerType,
          clientId,
          coordinatorId,
          sendTimeMs,
          createTimeMs,
          requestTimeoutMs,
          obj,
          watcher);
    }

    public ChaosRequestBuilder serializerType(int type) {
      this.serializerType = type;
      return this;
    }

    public ChaosRequestBuilder version(String version) {
      this.version = version;
      return this;
    }

    public ChaosRequestBuilder requestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public ChaosRequestBuilder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public ChaosRequestBuilder requestType(ChaosRequestFactory.Type requestType) {
      this.requestType = requestType;
      return this;
    }

    public ChaosRequestBuilder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public ChaosRequestBuilder coordinatorId(String coordinatorId) {
      this.coordinatorId = coordinatorId;
      return this;
    }

    public ChaosRequestBuilder sendTimeMs(long sendTimeMs) {
      this.sendTimeMs = sendTimeMs;
      return this;
    }

    public ChaosRequestBuilder createTimeMs(long createTimeMs) {
      this.createTimeMs = createTimeMs;
      return this;
    }

    public ChaosRequestBuilder requestTimeoutMs(long requestTimeoutMs) {
      this.requestTimeoutMs = requestTimeoutMs;
      return this;
    }

    public ChaosRequestBuilder watcher(ChaosWatcher watcher) {
      this.watcher = watcher;
      return this;
    }
  }
}
