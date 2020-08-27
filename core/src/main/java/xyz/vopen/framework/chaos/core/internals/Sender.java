package xyz.vopen.framework.chaos.core.internals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.core.internals.context.ChaosContext;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.remoting.aio.ChaosNetwork;
import xyz.vopen.framework.chaos.remoting.api.Session;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link Sender} The background thread that handles the sending of produce requests to le
 * coordinator.This thread makes metadata requests to renew its view of the groupCoordinator and
 * then sends produce requests to the appropriate nodes.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
public class Sender implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Sender.class);

  private static final long METADATA_UPDATE_TIME = 10000;

  private final RecordAccumulator accumulator;
  private ChaosMetadata chaosMetadata;
  private ChaosNetwork chaosNetwork;
  private ChaosConfig config;
  private ReentrantLock lock;
  private Condition notFull;

  private volatile boolean running = false;

  public Sender(ChaosContext context, ChaosNetwork chaosNetwork, ChaosConfig config) {
    this.config = config;
    this.lock = context.getNotFullLock();
    this.notFull = context.getNotFullCondition();
    this.accumulator = context.getAccumulator();
    this.chaosMetadata = new ChaosMetadata(config);
    this.chaosNetwork = chaosNetwork;
    this.running = true;
  }

  /**
   * This method not a real send operator, in this method that do some preparatory work before send
   * the request.
   */
  public void send() {
    try {
      lock.lockInterruptibly();

      if (accumulator.count() <= 0) {
        notFull.await();
      }

      long lastUpdateMetadata = this.chaosMetadata.getLastUpdateMetadata();

      // all active service
      List<InetSocketAddress> activeServices = this.chaosMetadata.getActiveServices();
      //    if (CollectionUtils.isEmpty(activeServices)i
      //        || Instant.now().toEpochMilli() - lastUpdateMetadata > METADATA_UPDATE_TIME) {
      //      LOGGER.warn("need update chaos server is metadata.");
      //      updateMetadata(notNew);
      //      try {
      //        notNew.await();
      //      } catch (InterruptedException e) {
      //        e.printStackTrace();
      //      }
      //      if (lastUpdateMetadata
      //          == this.chaosMetadata.getLastUpdateMetadata().toInstant().toEpochMilli()) {
      //        LOGGER.warn("Sender thread update metadata failure.");
      //        return;
      //      }
      //    }

      if (CollectionUtils.isEmpty(activeServices = this.chaosMetadata.getActiveServices())) {
        LOGGER.warn("Current chaos server unavailable");
        return;
      }

      // current available service.
      List<InetSocketAddress> services = activeServices;

      // ready to send request.
      ChaosRequest chaosRequest = this.accumulator.completeNext();
      if (chaosRequest == null) {
        return;
      }

      send0(services, chaosRequest);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      lock.unlock();
    }
  }

  /** Send updateMetadata request, sender thread will block util the response is received. */
  private void updateMetadata(Condition notNew) {}

  /**
   * Inner send request method.
   *
   * @param peers service node list.
   * @param chaosRequest sent request.
   */
  private void send0(List<InetSocketAddress> peers, ChaosRequest chaosRequest) {
    if (CollectionUtils.isEmpty(peers)) {
      return;
    }
    for (InetSocketAddress peer : peers) {
      Session session = this.chaosNetwork.getSessionMap(peer);
      if (session == null && this.config.isCoordinator()) {
        session = this.chaosNetwork.connect(peer);
        this.chaosNetwork.putSession(peer, session);
      }
      if (session != null) {
        boolean res = this.chaosNetwork.send(chaosRequest, session);
        if (res) {
          // add to already set.
          this.accumulator.addToAlreadySent(chaosRequest);
        }
      }
    }
  }

  @Override
  public void run() {
    try {
      while (running) {
        send();
      }
    } catch (Exception e) {
      LOGGER.error("chaos sender request occur error : {}", e.getMessage(), e);
    }
  }
}
