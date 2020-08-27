package xyz.vopen.framework.chaos.core.internals.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager;
import java.time.Instant;

/**
 * {@link ChaosRequestFactory} Build factories for {@link ChaosRequest} and {@link ChaosResponse}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/20
 */
public class ChaosRequestFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChaosRequestFactory.class);

  private ChaosConfig config;

  public ChaosRequestFactory(ChaosConfig config) {
    this.config = config;
  }

  /**
   * Returns the {@link ChaosRequest} object.
   *
   * @param type request type.
   */
  public ChaosRequest getRequest(Type type) {
    ChaosRequest.ChaosRequestBuilder builder =
        ChaosRequest.builder()
            .serviceName(this.config.getServiceName())
            .requestType(type)
            .requestId(String.valueOf(Instant.now().toEpochMilli()))
            .clientId(this.config.getClientId())
            .createTimeMs(Instant.now().toEpochMilli())
            .requestTimeoutMs(this.config.getRequestTimeout())
            .serializerType(this.config.getSerializerType());
    if (config.isCoordinator()) {
      builder.coordinatorId("coordinator - " + this.config.getClientId());
    }
    return builder.build();
  }

  /**
   * Returns the {@link ChaosResponse} object.
   *
   * @param type response type.
   */
  public ChaosResponse getResponse(Type type, boolean watcher, ChaosEventManager.ChaosEvent event) {
    ChaosResponse.ChaosResponseBuilder builder = ChaosResponse.builder();
    builder
        .clientId(event.getEventName())
        .requestId(event.getEventId())
        .requestType(type) // not real start.
        .createTimeMs(event.getCreateTime())
        .sendTimeMs(Instant.now().toEpochMilli())
        .serializerType(this.config.getSerializerType())
        .watch(watcher);
    return builder.build();
  }

  public enum Type {
    HEARTBEAT(1),
    OMINOUS(2),
    OFFLINE(3),
    ONLINE(4),
    SYNC(5),
    INIT(6),
    STARTED(7),
    EXIT(8),
    CONFIRM(9),
    SHAKEHAND(10);

    private int type;

    Type(int type) {
      this.type = type;
    }

    public int getType() {
      return type;
    }

    public void setType(int type) {
      this.type = type;
    }
  }
}
