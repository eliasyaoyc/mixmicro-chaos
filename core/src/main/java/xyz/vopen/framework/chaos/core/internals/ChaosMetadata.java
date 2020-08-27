package xyz.vopen.framework.chaos.core.internals;

import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.remoting.aio.ChaosServerConfig;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link ChaosMetadata}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/17
 */
public class ChaosMetadata {
  /** Last time that update metadata. */
  private long lastUpdateMetadata;

  /** The active service list. */
  private List<InetSocketAddress> activeServices;

  /** The dead service list. */
  private List<InetSocketAddress> negativeServices;

  public ChaosMetadata(ChaosConfig config) {
    this(
        Instant.now().toEpochMilli(),
        Arrays.stream(config.getServices().split(","))
            .map(s -> new InetSocketAddress(s.split(":")[0], Integer.parseInt(s.split(":")[1])))
            .collect(Collectors.toList()),
        new ArrayList<>());
  }

  public ChaosMetadata(
      long lastUpdateMetadata,
      List<InetSocketAddress> activeServices,
      List<InetSocketAddress> negativeServices) {
    this.lastUpdateMetadata = lastUpdateMetadata;
    this.activeServices = activeServices;
    this.negativeServices = negativeServices;
  }

  public ChaosMetadata(List<InetSocketAddress> activeServices) {
    this.activeServices = activeServices;
  }

  public long getLastUpdateMetadata() {
    return lastUpdateMetadata;
  }

  public void setLastUpdateMetadata(long lastUpdateMetadata) {
    this.lastUpdateMetadata = lastUpdateMetadata;
  }

  public List<InetSocketAddress> getActiveServices() {
    return activeServices;
  }

  public void setActiveServices(List<InetSocketAddress> activeServices) {
    this.activeServices = activeServices;
  }

  public List<InetSocketAddress> getNegativeServices() {
    return negativeServices;
  }

  public void setNegativeServices(List<InetSocketAddress> negativeServices) {
    this.negativeServices = negativeServices;
  }
}
