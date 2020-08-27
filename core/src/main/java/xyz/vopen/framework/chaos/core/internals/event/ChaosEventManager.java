package xyz.vopen.framework.chaos.core.internals.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import xyz.vopen.framework.chaos.common.utilities.TimerTaskUtility;
import xyz.vopen.framework.chaos.core.exception.MetadataInitializerException;
import xyz.vopen.framework.chaos.core.exception.MetadataStorageException;
import xyz.vopen.framework.chaos.core.internals.ChaosMetadata;
import xyz.vopen.framework.chaos.core.internals.EventManager;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory;
import xyz.vopen.framework.chaos.core.internals.request.ChaosResponse;
import xyz.vopen.framework.chaos.core.log.LogEntry;
import xyz.vopen.framework.chaos.core.log.LogManager;
import xyz.vopen.framework.chaos.remoting.aio.ChaosServerConfig;
import xyz.vopen.framework.chaos.remoting.api.Protocol;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type.*;
import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type;

/**
 * {@link ChaosEventManager} Storage all events from the client. same as metadata.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
public class ChaosEventManager implements EventManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChaosEventManager.class);

  private static final String COORDINATOR = "coordinator";
  private static final String ACTIVATION_THREAD_NAME = "chaos-server-activation";
  private static final int ASYNC_FLUSH = 2;
  private static final int MAX_RETRY_COUNT = 3;
  /** The interval between sending a message and the last sent. */
  private static final int SENT_INTERVAL = 5000;

  private final LogManager logManager;
  private ChaosMetadata metadata;
  private ActivationEvent activationEvent;
  private Map<Session /*corresponding client id*/, SessionPair> sessionMaps;
  private Map<String /*service name*/, Object> recordMaps;
  private final ChaosRequestFactory requestFactory;
  private final Protocol protocol;
  private final ReentrantReadWriteLock rwl;

  public ChaosEventManager(
      ChaosServerConfig config, ChaosRequestFactory requestFactory, Protocol protocol) {
    this.sessionMaps = new HashMap<Session, SessionPair>(12);
    this.recordMaps = new HashMap<String, Object>(12);
    this.requestFactory = requestFactory;
    this.protocol = protocol;
    this.metadata = new ChaosMetadata();
    this.activationEvent = new ActivationEvent();
    this.rwl = new ReentrantReadWriteLock();
    this.logManager =
        StringUtils.isEmpty(config.getLogDir())
            ? new LogManager(LogManager.getDefaultLogDir(config.getPort()))
            : new LogManager(config.getLogDir());

    // scheduled thread.
    TimerTaskUtility.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(
        activationEvent, 2, ASYNC_FLUSH, TimeUnit.SECONDS);
  }

  /**
   * Returns the corresponding {@link ChaosEvent} queue by given serviceName.
   *
   * @param request
   * @param session
   * @return
   */
  @Override
  public Queue<ChaosEvent> materialize(ChaosRequest request, Session session) {
    return materialize(request.getServiceName());
  }

  private Queue<ChaosEvent> materialize(String serviceName) {
    try {
      this.rwl.readLock().lockInterruptibly();
      if (StringUtils.isEmpty(serviceName)) {
        throw new IllegalArgumentException("Unknown service.");
      }

      Queue<ChaosEvent> chaosEvents =
          this.metadata
              .getAllEvents()
              .getOrDefault(serviceName, new ConcurrentLinkedDeque<ChaosEvent>());
      return chaosEvents;
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.rwl.readLock().unlock();
    }
    return null;
  }

  /**
   * Add a watcher to the {@link ChaosEventManager#metadata} if request id is the same then no
   * operator.
   *
   * @param request
   * @param session
   * @return
   */
  @Override
  public void add(ChaosRequest request, Session session) {
    try {
      this.rwl.writeLock().lockInterruptibly();
      ChaosEvent chaosEvent = convertToEvent(request, session);
      Queue<ChaosEvent> queue = this.materialize(request.getServiceName());
      boolean add = true;
      if (queue != null && queue.size() > 0) {
        for (ChaosEvent event : queue) {
          if (event.getEventType().equals(chaosEvent.getEventType())
              && event.getEventId().equals(chaosEvent.getEventId())) {
            add = false;
          }
        }
      }
      if (add) {
        boolean success = queue.add(chaosEvent);
        this.metadata.getAllEvents().put(request.getServiceName(), queue);
        if (!success) {
          throw new MetadataStorageException("event add failure.");
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.rwl.writeLock().unlock();
    }
  }

  /**
   * Count the number of current Coordinator nodes.
   *
   * @return
   */
  private int countCoordinatorNode() {
    if (CollectionUtils.isEmpty(this.sessionMaps)) {
      return 0;
    }

    long coordinator =
        this.sessionMaps.entrySet().stream()
            .filter(entry -> entry.getKey().getSessionId().startsWith(COORDINATOR))
            .count();

    return (int) coordinator;
  }

  private @NotNull ChaosEvent convertToEvent(ChaosRequest request, Session session) {
    return convertToEvent(request, session, 0);
  }

  private @NotNull ChaosEvent convertToEvent(ChaosRequest request, Session session, int count) {
    return new ChaosEvent(
        request.getClientId(),
        request.getRequestId(),
        request.getRequestType(),
        Instant.now().toEpochMilli(),
        session.getSessionId(),
        count,
        request.getSendTimeMs());
  }

  /**
   * Main method. In {@link ChaosEventManager#metadata} check the expired events and return the Exit
   * response notify target node exit. if corresponding service have no {@link
   * ChaosEvent#isTrigger()} is true then pick any one that send ready request.
   *
   * @param serviceName the name of set.
   * @return {@link ChaosResponse} Object.
   */
  private void checkInvalidEvents(String serviceName) {

    boolean sent = true;

    Queue<ChaosEvent> chaosEvents = this.materialize(serviceName);
    if (CollectionUtils.isEmpty(chaosEvents)) {
      return;
    }

    List<ChaosEvent> running = new ArrayList<>(chaosEvents.size());

    Iterator<ChaosEvent> events = chaosEvents.iterator();

    while (events.hasNext()) {
      ChaosEvent event = events.next();

      // if corresponding session is unReachable, remove event.
      if (isUnreachable(event.getSessionId())) {
        events.remove();
      }

      // are there any that have been sent but haven't received a response yet.
      if (event.isSent() && !event.isTrigger()) {
        if (event.getRenewalTime() + event.getMaximumTimeout() > Instant.now().toEpochMilli()) {
          sent = false;
          // timeout. retry
          if (event.getRetryCount() < MAX_RETRY_COUNT) {
            if (event.getSentTime() + SENT_INTERVAL <= Instant.now().toEpochMilli()) {
              doSend(serviceName, event, INIT);
            }
          } else {
            // beyond maxRetryCount, send exit ?
            events.remove();
            // add to send set.
            doSend(serviceName, event, EXIT);
          }
        }
      } else if (event.isSent && event.isTrigger) {
        running.add(event);
        sent = false;
      } else if (!event.isSent && event.isTrigger) {
        // occur error exit.
        events.remove();
        // add to send set.
        doSend(serviceName, event, EXIT);
      }
    }
    // multiple run nodes, saving the latest node and sending the remaining exit request for
    // eviction.
    if (running.size() > 1) {
      sent = false;
      // multiple nodes at keep one of the remaining all send exits.
      ChaosEvent keepEvent = null;
      long renewalTime = -1;
      for (ChaosEvent rEvent : running) {
        if (rEvent.getRenewalTime() > renewalTime) {
          renewalTime = rEvent.getRenewalTime();
          keepEvent = rEvent;
        }
      }
      running.remove(keepEvent);
      // eviction.
      running.stream()
          .forEach(
              r -> {
                // add to send set.
                doSend(serviceName, r, EXIT);
              });
    }

    if (sent) {
      ChaosEvent event = this.materialize(serviceName).peek();

      if (event != null && event.getRetryCount() < MAX_RETRY_COUNT) {
        event.setSent(true);
        if (event.getRetryCount() == 0) {
          // first send.
          doSend(serviceName, event, INIT);
        } else if (event.getSentTime() + SENT_INTERVAL <= Instant.now().toEpochMilli()) {
          doSend(serviceName, event, INIT);
        }
      }
    }
  }

  /**
   * Take request add to send queue.
   *
   * @param event {@link ChaosEvent}.
   */
  private void doSend(String serviceName, ChaosEvent event, Type type) {
    try {
      this.rwl.writeLock().lockInterruptibly();
      ChaosResponse response = requestFactory.getResponse(type, true, event);
      if (type == INIT) {
        int i = event.getRetryCount() + 1;
        event.setRetryCount(i);
        event.setSentTime(Instant.now().toEpochMilli());
        if (!CollectionUtils.isEmpty(this.recordMaps) && this.recordMaps.get(serviceName) != null) {
          response.setObj(this.recordMaps.get(serviceName));
          this.recordMaps.remove(serviceName);
        }
      }

      // add to send set.
      addToSendQueue(
          this.metadata.getNeedSent(),
          event.getEventId(),
          new ChaosRequestPair(response, event.getSessionId()));
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.rwl.writeLock().unlock();
    }
  }

  /**
   * Send a sync request to another coordinator node.
   *
   * @param request need sync request.
   */
  private void sync(Request request) {
    if (CollectionUtils.isEmpty(this.sessionMaps)) {
      return;
    }
    this.sessionMaps.entrySet().stream()
        .forEach(
            session -> {
              if (session.getKey().getSessionId().startsWith(COORDINATOR)) {
                addToSendQueue(
                    this.metadata.getSyncQueue(),
                    request.getRequestId(),
                    new ChaosRequestPair(request, session.getKey().getSessionId()));
              }
            });
  }

  /**
   * Add {@link ChaosRequestPair} to {@link ChaosMetadata#needSent}.
   *
   * @param eventId
   * @param pair ChaosResponsePair.
   */
  private void addToSendQueue(
      ArrayBlockingQueue<ChaosRequestPair> queue, String eventId, ChaosRequestPair pair) {
    if (pair == null || StringUtils.isEmpty(eventId)) {
      return;
    }
    if (!isDuplicate(queue, eventId, pair.getSessionId())) {
      queue.offer(pair);
    }
  }

  /**
   * Determine if there are the same pending request in sentSet.
   *
   * @param requestId
   * @return
   */
  private boolean isDuplicate(Queue<ChaosRequestPair> queue, String requestId, String sessionId) {
    if (CollectionUtils.isEmpty(queue)) {
      return false;
    }
    ChaosRequestPair pair =
        queue.stream()
            .filter(
                resPair ->
                    resPair.getRequest().getRequestId().equals(requestId)
                        && resPair.getSessionId().equals(sessionId))
            .findAny()
            .orElse(null);
    if (pair == null) {
      return false;
    }
    return true;
  }

  /**
   * It is called when the exit instruction is received.
   *
   * <p>1.remove all corresponding {@link ChaosEvent} in {@link ChaosMetadata#allEvents} Set.
   *
   * <p>2.remove all corresponding {@link ChaosEvent} in {@link ChaosMetadata#needSent} Set.
   *
   * <p>3.Determine if {@link ChaosRequest} field obj is not null. if there is a value,it is passed
   * to the next client in the same ServiceName,if no client in the ServiceName then store and
   * waiting.
   *
   * @param request {@link ChaosRequest} object.
   * @param session
   */
  @Override
  public void removeEvent(ChaosRequest request, Session session) {
    try {
      this.rwl.writeLock().lockInterruptibly();
      Queue<ChaosEvent> queue = this.materialize(request, session);
      boolean del = false;
      ChaosEvent event = null;
      if (queue != null) {
        Iterator<ChaosEvent> events = queue.iterator();
        while (events.hasNext()) {
          ChaosEvent next = events.next();
          if (next.getSessionId().equals(session.getSessionId())) {
            event = next;
            del = true;
          }
        }
        // received a EXIT command, remove corresponding ChaosEvent and send EXIT callback.
        if (del && event != null) {
          // remove all corresponding ChaosEvent.
          clearUpEvents(request.getServiceName(), request.getClientId());
          clearUpSentEvents(request.getServiceName(), session.getSessionId());

          // part 3
          if (!ObjectUtils.isEmpty(request.getObj())) {
            this.recordMaps.put(request.getServiceName(), request.getObj());
          }

          // add to send set.
          addToSendQueue(
              this.metadata.getNeedSent(),
              event.getEventId(),
              new ChaosRequestPair(
                  requestFactory.getResponse(EXIT, true, event), event.getSessionId()));
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.rwl.writeLock().unlock();
    }
  }

  /**
   * Received a Exit request and cleanup all corresponding events.
   *
   * @param serviceName need to clean up.
   */
  private void clearUpEvents(String serviceName, String clientId) {
    if (CollectionUtils.isEmpty(this.metadata.getAllEvents())) {
      return;
    }
    Queue<ChaosEvent> events = this.metadata.getAllEvents().get(serviceName);
    Iterator<ChaosEvent> iterator = events.iterator();
    if (iterator.hasNext()) {
      ChaosEvent next = iterator.next();
      if (next.getEventName().equals(clientId)) {
        iterator.remove();
      }
    }
  }

  /**
   * Received a Exit request and cleanup all corresponding events in sent queue.
   *
   * @param serviceName
   * @param sessionId
   */
  private void clearUpSentEvents(String serviceName, String sessionId) {
    if (CollectionUtils.isEmpty(this.metadata.getNeedSent())) {
      return;
    }
    Queue<ChaosRequestPair> needSent = this.metadata.getNeedSent();
    Iterator<ChaosRequestPair> iterator = needSent.iterator();
    if (iterator.hasNext()) {
      ChaosRequestPair next = iterator.next();
      if (next.getSessionId().equals(sessionId)) {
        iterator.remove();
      }
    }
  }

  /**
   * Update specified {@link ChaosRequest} through heartbeat request. A service node is considered
   * dead to send an Exit request and a ready request to another service node if it has not been
   * updated for a long time.
   *
   * @param request
   * @param session
   */
  @Override
  public void update(ChaosRequest request, Session session) {
    try {
      this.rwl.writeLock().lockInterruptibly();
      if (session.getSessionId() != null) {
        // first，renewal session.
        this.renewal(session);
      } else {
        // second，renewal request.
        if (this.getSessionMaps().containsKey(session)) {
          ChaosEvent event = convertToEvent(request, session);
          Queue<ChaosEvent> queue = this.materialize(request, session);
          Iterator<ChaosEvent> iterator = queue.iterator();
          while (iterator.hasNext()) {
            ChaosEvent next = iterator.next();
            if (next.getEventId() == event.getEventId()) {
              next.setRenewalTime(event.getRenewalTime());
            }
          }
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.rwl.writeLock().unlock();
    }
  }

  /**
   * Client node received a ready response and sent a confirm request.
   *
   * <p>the first case, not client running and direct sent a start response to corresponding client
   * node and trigger start method.
   *
   * <p>the second case, have client running then reset {@link ChaosEvent} and remove task from
   * {@link ChaosMetadata#needSent}.
   *
   * <p>This request need sync.
   *
   * @param response {@link ChaosResponse} object.
   * @param session
   */
  @Override
  public void confirm(ChaosRequest response, Session session) {

    try {
      this.rwl.writeLock().lockInterruptibly();
      if (CollectionUtils.isEmpty(this.metadata.allEvents)) {
        return;
      }

      Queue<ChaosEvent> materialize = materialize(response.getServiceName());
      if (materialize.isEmpty()) {
        return;
      }

      boolean isRunning = false;
      for (ChaosEvent event : materialize) {
        if (isRunning = event.isTrigger()) {
          break;
        }
      }

      Iterator<ChaosEvent> iterator = materialize.iterator();
      while (iterator.hasNext()) {
        ChaosEvent next = iterator.next();
        if (next.getEventId().equals(response.getRequestId()) && !isRunning) {

          // sent start request.
          next.setSent(true);
          next.setTrigger(true);
          next.setRenewalTime(Instant.now().toEpochMilli());
          next.setRetryCount(0);
          addToSendQueue(
              this.metadata.getNeedSent(),
              next.getEventId(),
              new ChaosRequestPair(
                  this.requestFactory.getResponse(STARTED, true, next), session.getSessionId()));
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.rwl.writeLock().unlock();
    }
  }

  /**
   * This method secure availability, a coordinator node crash read data from disk and invoke this
   * method.
   */
  @Override
  public void initialize() {
    try {
      // read data from disk.
      LogEntry logEntry = logManager.read(logManager.CHAOS_METADATA);
      if (logEntry == null) {
        return;
      }
      // handle data (e.g. eliminate expired data, structure object etc)
      ChaosMetadata metadata = logEntry.getMetadata();
      this.metadata = metadata;

    } catch (Exception e) {
      throw new MetadataInitializerException("Metadata initialize failure.", e);
    }
  }

  /** fault-tolerance. flush all data to disk. */
  @Override
  public void destroy() {
    asyncFlush(buildMetadata());
    this.logManager.close();
  }

  /**
   * Handle metadata that current node read form disk. At the same time, sent Sync request to
   * current active node to get their {@link ChaosMetadata} to update current {@link ChaosMetadata}.
   * Note that you only send one once.
   *
   * @param metadata the metadata that current node read from disk.
   */
  private void handleEntry(ChaosMetadata metadata) {

    if (metadata == null) {
      return;
    }

    Map<Session, SessionPair> sessionMaps = this.getSessionMaps();
    if (CollectionUtils.isEmpty(sessionMaps)) {
      return;
    }

    Iterator<Map.Entry<Session, SessionPair>> iterator = sessionMaps.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Session, SessionPair> next = iterator.next();
      if (next.getKey().isInvalid()) {
        iterator.remove();
      }
      this.protocol.send0(requestFactory.getRequest(SYNC), next.getKey());
    }
  }

  /** Async flush event to dist. */
  private void asyncFlush(LogEntry entry) {
    logManager.write(entry);
  }

  /** Build current Metadata async disk. */
  private LogEntry buildMetadata() {
    return buildMetadata(this.metadata);
  }

  private LogEntry buildMetadata(ChaosMetadata metadata) {
    return LogEntry.builder().serviceName(LogManager.CHAOS_METADATA).metadata(metadata).build();
  }

  @Override
  public synchronized ChaosRequestPair getResponse(boolean isSyncQueue) {
    try {
      ChaosRequestPair pair = null;
      if (isSyncQueue) {
//        if (!CollectionUtils.isEmpty(this.metadata.getSyncQueue())) {
          pair = this.metadata.getSyncQueue().take();
//        }
      } else {
//        if (!CollectionUtils.isEmpty(this.metadata.getNeedSent())) {
          pair = this.metadata.getNeedSent().take();
//        }
      }
      if (pair != null && isUnreachable(pair.getSessionId())) {
        return null;
      }
      return pair;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /** Remove session in {@link ChaosEventManager#metadata}. */
  @Override
  public void addUnreachableNode(Session session) {
    if (CollectionUtils.isEmpty(this.getSessionMaps())
        || !this.getSessionMaps().containsKey(session)) {
      return;
    }
    this.getSessionMaps().replace(session, new SessionPair(Instant.now().toEpochMilli(), true));
  }

  /** Determines whether a given session is unreachable */
  private boolean isUnreachable(String sessionId) {
    Session session = getSession(sessionId);
    if (session == null) {
      return true;
    }
    return false;
  }

  /** Add session to {@link ChaosEventManager#metadata}. */
  @Override
  public void addServiceNode(Session session) {
    LOGGER.info("session-id : {} join to sessionMaps ", session.getSessionId());
    if (CollectionUtils.isEmpty(this.getSessionMaps())) {
      this.getSessionMaps().put(session, new SessionPair(Instant.now().toEpochMilli(), false));
    }

    AtomicBoolean add = new AtomicBoolean(true);

    Iterator<Map.Entry<Session, SessionPair>> iterator =
        this.getSessionMaps().entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Session, SessionPair> sess = iterator.next();
      if (sess.getKey().getSessionId().equals(session.getSessionId())) {
        add.set(false);
        // re connection.
        if (sess.getValue().isUnreachable()) {
          add.set(true);
          LOGGER.warn("session-id : {} reconnection. remove old session", session.getSessionId());
          iterator.remove();
        }
      }
    }

    if (add.get()) {
      this.getSessionMaps().put(session, new SessionPair(Instant.now().toEpochMilli(), false));
    }
  }

  /**
   * Renewal session through {@link
   * xyz.vopen.framework.chaos.core.processor.chain.impl.HeartbeatHandler}.
   */
  public void renewal(Session session) {
    if (CollectionUtils.isEmpty(this.getSessionMaps())
        || !this.getSessionMaps().containsKey(session)) {
      return;
    }
    if (isUnreachable(session.getSessionId())) {
      LOGGER.error(
          "session-id : {} is unreachable, renewal invalid must reconnection.",
          session.getSessionId());
    } else {
      this.getSessionMaps().replace(session, new SessionPair(Instant.now().toEpochMilli(), false));
    }
  }

  /** Returns sessions. */
  @Override
  public Map<Session, SessionPair> getSessions() {
    return this.getSessionMaps();
  }

  public Map<Session, SessionPair> getSessionMaps() {
    return sessionMaps;
  }

  public void setSessionMaps(Map<Session, SessionPair> sessionMaps) {
    this.sessionMaps = sessionMaps;
  }
  /**
   * The thread that activation event， in terms of thread communication, if have thread invoke
   * method in {@link ChaosEventManager} and wakeup this thread. The validity of the detection event
   * is based on whether the difference between the updated event and the current time of the event
   * exceeds the maximum time limit.
   */
  public class ActivationEvent implements Runnable {

    @Override
    public void run() {
      try {
        // allEvents section.
        // detect expiration events.
        if (!CollectionUtils.isEmpty(metadata.getAllEvents())) {
          // update local memory (eliminate expired data.)
          Map<String, Queue<ChaosEvent>> events = metadata.getAllEvents();
          events.entrySet().stream().forEach(event -> checkInvalidEvents(event.getKey()));
          // flush disk.
          asyncFlush(buildMetadata());
        }
      } catch (Exception e) {
        LOGGER.error("Async flush metadata error : {}", e);
      }
    }
  }

  @Override
  public Session getSession(String sessionId) {
    if (CollectionUtils.isEmpty(this.getSessionMaps())) {
      return null;
    }

    Map.Entry<Session, SessionPair> pairEntry =
        this.getSessionMaps().entrySet().stream()
            .filter(
                entry ->
                    entry.getKey().getSessionId().equals(sessionId)
                        && !entry.getValue().isUnreachable())
            .findFirst()
            .orElse(null);

    if (pairEntry == null) {
      return null;
    }
    return pairEntry.getKey();
  }

  /**
   * When receive a sync response indicating that the corresponding node has synchronized the
   * request, subtract the number of requests that need to be synchronized and return it to the
   * client when it is zero.
   *
   * @param response sync response.
   * @param session
   */
  @Override
  public void syncCallback(ChaosResponse response, Session session) {
    //    Queue<ChaosEvent> materialize = this.materialize(response.getServiceName());
    //    if (CollectionUtils.isEmpty(materialize)) {
    //      return;
    //    }
    //    materialize.stream()
    //        .forEach(
    //            event -> {
    //              if (event.getEventId().equals(response.getRequestId())) {
    //                int remain;
    //                if ((remain = event.getRemainCallbackCount()) > 0) {
    //                  remain--;
    //                  event.setRemainCallbackCount(remain);
    //                }
    //                if (remain <= 0) {
    //                  // case 1. online request sync then send ready response.
    //                  // case 2. confirm request sync then send start response.
    //                  ChaosResponse chaosResponse = null;
    //                  switch (event.getEventType()) {
    //                    case ONLINE:
    //                      chaosResponse = this.requestFactory.getResponse(INIT, true, event);
    //                      break;
    //                    case CONFIRM:
    //                      chaosResponse = this.requestFactory.getResponse(STARTED, true, event);
    //                      break;
    //                  }
    //                  //                  if (chaosResponse != null) {
    //                  //                    addToSendQueue(
    //                  //                        this.metadata.getNeedSent(),
    //                  //                        event.getEventId(),
    //                  //                        new ChaosRequestPair(chaosResponse,
    //                  // event.getSessionId()));
    //                  //                  }
    //                }
    //              }
    //            });
  }

  /** Manager Chaos metadata. */
  public static class ChaosMetadata implements Serializable {
    /**
     * Storage the event from client (key : serviceName, value : a collection of all events
     * belonging to the service).
     */
    private @NotNull Map<String, Queue<ChaosEvent>> allEvents = new ConcurrentHashMap<>();

    private @NotNull ArrayBlockingQueue<ChaosRequestPair> needSent =
        new ArrayBlockingQueue<ChaosRequestPair>(512);

    private @NotNull ArrayBlockingQueue<ChaosRequestPair> syncQueue =
        new ArrayBlockingQueue<ChaosRequestPair>(512);

    public ArrayBlockingQueue<ChaosRequestPair> getSyncQueue() {
      return syncQueue;
    }

    public void setSyncQueue(ArrayBlockingQueue<ChaosRequestPair> syncQueue) {
      this.syncQueue = syncQueue;
    }

    public Map<String, Queue<ChaosEvent>> getAllEvents() {
      return allEvents;
    }

    public void setAllEvents(Map<String, Queue<ChaosEvent>> allEvents) {
      this.allEvents = allEvents;
    }

    public ArrayBlockingQueue<ChaosRequestPair> getNeedSent() {
      return needSent;
    }

    public void setNeedSent(ArrayBlockingQueue<ChaosRequestPair> needSent) {
      this.needSent = needSent;
    }
  }

  public static class SessionPair implements Serializable {
    long renewalTime;
    boolean isUnreachable;

    public SessionPair(long renewalTime, boolean isUnreachable) {
      this.renewalTime = renewalTime;
      this.isUnreachable = isUnreachable;
    }

    public long getRenewalTime() {
      return renewalTime;
    }

    public void setRenewalTime(long renewalTime) {
      this.renewalTime = renewalTime;
    }

    public boolean isUnreachable() {
      return isUnreachable;
    }

    public void setUnreachable(boolean unreachable) {
      isUnreachable = unreachable;
    }
  }

  public static class ChaosRequestPair implements Serializable {
    private Request chaosRequest;
    private String sessionId;

    public ChaosRequestPair(Request chaosRequest, String sessionId) {
      this.chaosRequest = chaosRequest;
      this.sessionId = sessionId;
    }

    public Request getRequest() {
      return chaosRequest;
    }

    public String getSessionId() {
      return sessionId;
    }
  }

  /** Manager all events metadata. */
  public static class ChaosEvent implements Serializable {

    /** The name of the Event which has created the event. */
    private String eventName;

    private String eventId;

    /** The type of event. */
    private ChaosRequestFactory.Type eventType;

    /** The creation time of event. */
    private long createTime;

    /** The time of the latest interface heartbeat request. */
    private long renewalTime;

    /** The event that the maximum timeout (unit millisecond). */
    private long maximumTimeout;

    /** The event that the retry count. */
    private int retryCount;

    /** whether sent. */
    private boolean isSent;

    /** The time of the send request. */
    private long sentTime;

    /** Whether trigger. */
    private boolean isTrigger;

    private String sessionId;

    public ChaosEvent(
        String eventName,
        String eventId,
        ChaosRequestFactory.Type eventType,
        long createTime,
        String sessionId,
        long sentTime) {
      this(eventName, eventId, eventType, createTime, sessionId, 0, sentTime);
    }

    public ChaosEvent(
        String eventName /*client id*/,
        String eventId /*request id*/,
        ChaosRequestFactory.Type eventType,
        long createTime,
        String sessionId,
        int remainCallbackCount,
        long sentTime) {
      this.eventId = eventId;
      this.eventName = eventName;
      this.eventType = eventType;
      this.createTime = createTime;
      this.renewalTime = createTime;
      this.maximumTimeout = 6000;
      this.retryCount = 0;
      this.isSent = false;
      this.isTrigger = false;
      this.sessionId = sessionId;
      this.sentTime = sentTime;
    }

    public long getSentTime() {
      return sentTime;
    }

    public void setSentTime(long sentTime) {
      this.sentTime = sentTime;
    }

    public String getEventName() {
      return eventName;
    }

    public String getEventId() {
      return eventId;
    }

    public void setEventId(String eventId) {
      this.eventId = eventId;
    }

    public void setEventName(String eventName) {
      this.eventName = eventName;
    }

    public ChaosRequestFactory.Type getEventType() {
      return eventType;
    }

    public void setEventType(ChaosRequestFactory.Type eventType) {
      this.eventType = eventType;
    }

    public long getCreateTime() {
      return createTime;
    }

    public void setCreateTime(long createTime) {
      this.createTime = createTime;
    }

    public long getRenewalTime() {
      return renewalTime;
    }

    public void setRenewalTime(long renewalTime) {
      this.renewalTime = renewalTime;
    }

    public long getMaximumTimeout() {
      return maximumTimeout;
    }

    public void setMaximumTimeout(long maximumTimeout) {
      this.maximumTimeout = maximumTimeout;
    }

    public int getRetryCount() {
      return retryCount;
    }

    public void setRetryCount(int retryCount) {
      this.retryCount = retryCount;
    }

    public boolean isSent() {
      return isSent;
    }

    public void setSent(boolean sent) {
      isSent = sent;
    }

    public boolean isTrigger() {
      return isTrigger;
    }

    public void setTrigger(boolean trigger) {
      isTrigger = trigger;
    }

    public String getSessionId() {
      return sessionId;
    }

    public void setSession(String session) {
      this.sessionId = session;
    }

    public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
    }
  }
}
