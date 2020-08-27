package xyz.vopen.framework.chaos.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import xyz.vopen.framework.chaos.common.exception.ChaosException;
import xyz.vopen.framework.chaos.common.utilities.ThreadFactoryUtility;
import xyz.vopen.framework.chaos.common.utilities.TimerTaskUtility;
import xyz.vopen.framework.chaos.core.ChaosCoordinator;
import xyz.vopen.framework.chaos.core.exception.ChaosRequestException;
import xyz.vopen.framework.chaos.core.internals.ChaosServerContext;
import xyz.vopen.framework.chaos.core.internals.RecordAccumulator;
import xyz.vopen.framework.chaos.core.internals.Sender;
import xyz.vopen.framework.chaos.core.internals.context.AbstractChaosContext.*;
import xyz.vopen.framework.chaos.core.internals.event.ChaosWatcher;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type;
import xyz.vopen.framework.chaos.remoting.aio.AbstractChaosServer;
import xyz.vopen.framework.chaos.remoting.aio.ChaosNetwork;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type.*;

/**
 * {@link ChaosClient}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/25
 */
public class ChaosClient extends AbstractChaosServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChaosClient.class);

  private static final String CHAOS_SENDER = "Chaos-sender";
  private ChaosNetwork chaosNetwork;
  private ChaosClientContext context;
  private Sender sender;
  private ChaosClientConfig clientConfig;
  private boolean running = false;

  public ChaosClient(ChaosClientConfig config) {
    this(config, null);
  }

  public ChaosClient(ChaosClientConfig config, ChaosClientContext context) {
    if (context == null) {
      // build context.
      context = new ChaosClientContext(config);
    }
    this.context = context;
    this.clientConfig = (ChaosClientConfig) context.getConfig();
    preInit();
  }

  /**
   * Returns the {@link ChaosServerContext}.
   *
   * @return
   */
  public ChaosClientContext getContext() {
    return this.context;
  }

  @Override
  public void preInit() {
    // additional heartbeat plugin.
    this.chaosNetwork = new ChaosNetwork(clientConfig);

    this.sender = new Sender(context, this.chaosNetwork, clientConfig);

    Thread thread = ThreadFactoryUtility.createFactory(CHAOS_SENDER).newThread(this.sender);
    thread.start();
  }

  @Override
  public void start() {
    super.start();
    if (this.chaosNetwork == null) {
      throw new ChaosException("chaos client must not be empty... please check it.");
    }
    this.chaosNetwork.start();
    if (chaosNetwork.getSession() != null) {
      LOGGER.info("Chaos start success...");
    } else {
      throw new ChaosException("Chaos start occur exception... please check it.");
    }
    Session session = chaosNetwork.getSessionForChaos();
    if (session == null) {
      throw new ChaosException("Chaos server started failure.");
    }
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  public Session connect(InetSocketAddress address) {
    // register the reconnect event for this connection.
    if (this.chaosNetwork != null) {
      Session session = this.chaosNetwork.connect(address);
      this.chaosNetwork.putSession(address, session);
      //      wakeup();
      return session;
    }
    return null;
  }

  /**
   * The main method, send register request to {@link ChaosCoordinator} and trigger a callback when
   * a conditional exceeded.
   *
   * @param watcher callback.
   */
  public void register(ChaosWatcher watcher) {
    if (watcher == null) {
      throw new IllegalArgumentException("The argument of ChaosWatcher must not be empty.");
    }
    this.send(null, null, ONLINE, watcher);
  }

  /** ack method, confirm received response from {@link ChaosCoordinator}. */
  public void ack(String cursor) {
    if (StringUtils.isEmpty(cursor)) {
      throw new IllegalArgumentException("Cursor must be not empty.");
    }
    this.send(null, cursor, CONFIRM, null);
  }

  /** Sent an failure request when an exception occurs in its own service. */
  public void failure(Object obj) {
    this.send(obj, null, OMINOUS, null);
  }

  /** Sent an failure request when an exception occurs in its own service. */
  public void failure() {
    this.send(null, null, OMINOUS, null);
  }

  /**
   * Used by clients to send messages.
   *
   * @param cursor used for ack equivalent requestId.
   * @param requestType
   * @param watcher
   */
  private void send(Object obj, String cursor, Type requestType, ChaosWatcher watcher) {
    if (!running) {
      startClient();
    }
    if (requestType == null) {
      throw new ChaosException("The argument of requestType that must not empty.");
    }

    ChaosRequest request = this.context.getRequestFactory().getRequest(requestType);
    if (request == null) {
      throw new ChaosRequestException(
          "Get the chaos request failure, check the correctness that input request type.");
    }

    // continue assembly request.
    if (watcher != null) {
      request.setWatcher(watcher);
    }
    if (!StringUtils.isEmpty(cursor)) {
      request.setRequestId(cursor);
    }

    if (!ObjectUtils.isEmpty(obj)) {
      request.setObj(obj);
    }

    send(request);
  }

  /**
   * Used by Coordinator node send sync request.
   *
   * @param request sync request.
   */
  public void send(Request request) {

    // add request to accumulator.
    RecordAccumulator.RecordAppendResult result = this.context.getAccumulator().add(request);

    if (result.abortForEnqueue || result.maturity) {
      LOGGER.warn("Chaos request add to accumulator failure, please note that.");
    }

    if (result.enqueue) {
      LOGGER.info(
          "Waking up the sender since chaos request is either full or getting a new batch.");

      wakeup();
    }
  }

  /** Wake up the sender thread. */
  private void wakeup() {
    if (this.chaosNetwork.isWakeup()) {
      this.context.wakeupNotFull();
    }
  }

  /**
   * Connect given addresses, if the retry count is exceeded then drop it.
   *
   * @throws InterruptedException
   */
  public void startCoordinatorClient() {
    // start chaos.
    if (!StringUtils.isEmpty(this.clientConfig.getServices())) {

      CountDownLatch latch = new CountDownLatch(this.context.getConnectionSet().size());

      TimerTaskUtility.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(
          new TimerTask() {
            @Override
            public void run() {
              ConnectionPair pair;
              ConnectionSet connectionSet = context.getConnectionSet();
              if ((pair = context.getConnectionSet().peek()) != null) {
                InetSocketAddress address = pair.getAddress();
                int count;
                if ((count = pair.getRetry()) != -1) {
                  if (count == 0) {
                    connectionSet.remove(pair);
                    return;
                  }
                  pair.setRetry(count - 1);
                }
                Session session = connect(address);
                if (session != null) {
                  latch.countDown();
                  connectionSet.remove(pair);
                }
              }
            }
          },
          1,
          3,
          TimeUnit.SECONDS);

      try {
        latch.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Distinguish from the {@link ChaosClient#startCoordinatorClient()} this method will only connect
   * to one address.
   */
  public void startClient() {
    this.running = true;
    // start chaos.
    if (!StringUtils.isEmpty(this.clientConfig.getServices())) {

      TimerTaskUtility.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(
          new TimerTask() {
            @Override
            public void run() {
              ConnectionPair pair;
              ConnectionSet connectionSet = context.getConnectionSet();
              if ((pair = connectionSet.peek()) != null) {
                InetSocketAddress address = pair.getAddress();
                int count;
                if ((count = pair.getRetry()) != -1) {
                  if (count == 0) {
                    connectionSet.remove(pair);
                    return;
                  }
                  pair.setRetry(count - 1);
                }
                Session session = connect(address);
                if (session != null) {
                  connectionSet.reset();
                }
              }
            }
          },
          1,
          3,
          TimeUnit.SECONDS);
    }
  }
}
