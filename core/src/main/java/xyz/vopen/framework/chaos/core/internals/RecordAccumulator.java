package xyz.vopen.framework.chaos.core.internals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory;
import xyz.vopen.framework.chaos.core.internals.request.ChaosResponse;
import xyz.vopen.framework.chaos.remoting.api.Request;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type.*;

/**
 * {@link RecordAccumulator}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
public class RecordAccumulator {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecordAccumulator.class);

  private static final int INITIAL_CAPACITY = 10;

  /**
   * The set of requests which have been sent or are being sent but haven't yet received a response.
   */
  private Map<String /*client id*/, Queue<ChaosRequest>> readySent;

  private Map<String /*client id*/, Queue<ChaosRequest>> alreadySent;
  private final ChaosConfig config;
  private final AtomicInteger counter;
  private final ReentrantReadWriteLock rwl;

  public RecordAccumulator(ChaosConfig config) {
    this.config = config;
    this.readySent = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    this.alreadySent = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    this.counter = new AtomicInteger(0);
    this.rwl = new ReentrantReadWriteLock();
  }

  /**
   * Add the given request to the queue for the connection it was directed to.
   *
   * @return
   */
  public synchronized RecordAppendResult add(Request req) {
    ChaosRequest request = (ChaosRequest) req;
    Queue<ChaosRequest> deque =
        this.readySent.getOrDefault(request.getClientId(), new ArrayDeque<>(INITIAL_CAPACITY));
    deque.offer(request);
    this.readySent.put(request.getClientId(), deque);
    counter.incrementAndGet();
    return RecordAppendResult.ok();
  }

  /**
   * Return the oldest request (e.g. the one that will be completed next) .
   *
   * @return the oldest request.
   */
  public ChaosRequest completeNext() {
    String key = this.config.getClientId();
    return completeNext(key);
  }

  public ChaosRequest completeNext(String key) {
    try {
      this.rwl.readLock().lockInterruptibly();
      Queue<ChaosRequest> deque = this.readySent.get(key);
      if (!CollectionUtils.isEmpty(deque)) {
        ChaosRequest request = deque.peek();
        // set send time.
        request.setSendTimeMs(Instant.now().toEpochMilli());
        // check the set of alreadySent that whether timeout request.  if have resend.
        //      checkTimeoutRequest(key);
        return request;
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.rwl.readLock().unlock();
    }
    return null;
  }

  private void checkTimeoutRequest(String key) {
    if (CollectionUtils.isEmpty(this.alreadySent)) {
      return;
    }
    Queue<ChaosRequest> deque = this.alreadySent.get(key);
    if (CollectionUtils.isEmpty(deque)) {
      return;
    }
    deque.stream()
        .forEach(
            d -> {
              if (d.getSendTimeMs() + d.getRequestTimeoutMs() <= Instant.now().toEpochMilli()) {
                // timeout.
                this.readySent.getOrDefault(key, new ArrayDeque<>()).add(d);
                deque.remove(d);
              }
            });
  }

  private boolean checkSingleRequest(ChaosRequest request) {
    if (request.getSendTimeMs() + request.getRequestTimeoutMs() <= Instant.now().toEpochMilli()) {
      return true;
    } else {
      LOGGER.warn("Request : {}, timeout ", request.getClientId() + request.getRequestId());
      return false;
    }
  }

  public synchronized void addToAlreadySent(ChaosRequest request) {
    try {
      this.rwl.writeLock().lockInterruptibly();
      Queue<ChaosRequest> ready = this.readySent.get(request.getClientId());
      if (!CollectionUtils.isEmpty(ready)) {
        // remove ready set.
        ready.remove(request);
        counter.decrementAndGet();

        // add to already set.
        Queue<ChaosRequest> deque = this.alreadySent.get(request.getClientId());
        if (CollectionUtils.isEmpty(deque)) {
          Queue<ChaosRequest> de = new ArrayDeque<ChaosRequest>(INITIAL_CAPACITY);
          de.offer(request);
          this.alreadySent.put(request.getClientId(), de);
        } else {
          deque.add(request);
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.rwl.writeLock().unlock();
    }
  }

  /**
   * Return ChaosRequest object.
   *
   * @param response {@link ChaosResponse} object.
   * @return
   */
  public ChaosRequest getChaosRequest(ChaosResponse response) {
    try {
      this.rwl.readLock().lockInterruptibly();
      if (!CollectionUtils.isEmpty(this.alreadySent)) {
        Queue<ChaosRequest> sentRequests =
            this.alreadySent.getOrDefault(response.getClientId(), new ArrayDeque<>());
        Iterator<ChaosRequest> iterator = sentRequests.iterator();
        while (iterator.hasNext()) {
          ChaosRequest request = iterator.next();
          // TODO whether to set the timeout period.
          if (request.getRequestId().equals(response.getRequestId())
              && request.getRequestType() == ONLINE) {
            if (response.getRequestType() == EXIT) {
              this.alreadySent = new ConcurrentHashMap<>();
            }
            return request;
          }
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.rwl.readLock().unlock();
    }
    return null;
  }

  public void remove(String key) {
    this.readySent.remove(key);
  }

  /**
   * The count of {@link RecordAccumulator#readySent}.
   *
   * @return count.
   */
  public int count() {
    return counter.get();
  }

  /** Reset all already requests status to resend. */
  public void reset() {
    try {
      this.rwl.writeLock().lockInterruptibly();
      if (CollectionUtils.isEmpty(this.alreadySent)) {
        return;
      }
      Map<String, Queue<ChaosRequest>> newAlreadySent = this.alreadySent;
      this.alreadySent = new ConcurrentHashMap<>();
      this.readySent = new ConcurrentHashMap<>();

      for (Map.Entry<String, Queue<ChaosRequest>> entry : newAlreadySent.entrySet()) {
        int l = 0;
        Queue<ChaosRequest> queue = entry.getValue();
        if (!CollectionUtils.isEmpty(queue)) {
          l = queue.size();
          Deque<ChaosRequest> nQueue = new ArrayDeque<>(l);
          for (int i = 0; i < l; i++) {
            ChaosRequest chaosRequest = queue.poll();
            nQueue.offer(chaosRequest);
          }
          this.readySent.put(entry.getKey(), nQueue);
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.rwl.writeLock().unlock();
    }
  }

  /** Metadata about a record just appended to the record accumulator. */
  public static final class RecordAppendResult {
    public final boolean enqueue;
    public final boolean abortForEnqueue;
    public final boolean maturity;

    public RecordAppendResult(boolean enqueue, boolean abortForEnqueue, boolean maturity) {
      this.enqueue = enqueue;
      this.abortForEnqueue = abortForEnqueue;
      this.maturity = maturity;
    }

    static RecordAppendResult ok() {
      return new RecordAppendResult(true, false, false);
    }

    static RecordAppendResult abort() {
      return new RecordAppendResult(false, true, false);
    }

    static RecordAppendResult timeout() {
      return new RecordAppendResult(false, true, true);
    }
  }
}
