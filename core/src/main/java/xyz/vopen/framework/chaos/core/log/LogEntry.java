package xyz.vopen.framework.chaos.core.log;

import xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager;

import java.io.Serializable;

/**
 * {@link LogEntry}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/14
 */
public class LogEntry implements Serializable {

  private String serviceName;
  private ChaosEventManager.ChaosMetadata metadata;

  public LogEntry(String serviceName, ChaosEventManager.ChaosMetadata metadata){
    this.serviceName = serviceName;
    this.metadata = metadata;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public ChaosEventManager.ChaosMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ChaosEventManager.ChaosMetadata metadata) {
    this.metadata = metadata;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String serviceName;
    private ChaosEventManager.ChaosMetadata metadata;

    public Builder() {}

    public Builder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder metadata(ChaosEventManager.ChaosMetadata metadata){
      this.metadata = metadata;
      return this;
    }

    public LogEntry build() {
      return new LogEntry(this.serviceName,this.metadata);
    }
  }
}
