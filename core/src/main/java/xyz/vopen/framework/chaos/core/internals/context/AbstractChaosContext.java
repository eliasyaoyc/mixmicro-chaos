package xyz.vopen.framework.chaos.core.internals.context;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.core.internals.DefaultProtocol;
import xyz.vopen.framework.chaos.core.internals.EventManager;
import xyz.vopen.framework.chaos.core.internals.RecordAccumulator;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory;
import xyz.vopen.framework.chaos.core.internals.serializer.SerializerFactory;
import xyz.vopen.framework.chaos.core.plugins.HeartBeatPlugin;
import xyz.vopen.framework.chaos.core.plugins.MonitorPlugin;
import xyz.vopen.framework.chaos.core.processor.chain.Handler;
import xyz.vopen.framework.chaos.core.processor.chain.HandlerRegister;
import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.NetMonitor;
import xyz.vopen.framework.chaos.remoting.api.Plugin;
import xyz.vopen.framework.chaos.remoting.api.Protocol;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.Type.HEARTBEAT;

/**
 * {@link AbstractChaosContext} The {@link ChaosContext} based-Implementation, providers low-level
 * method.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/26
 */
public abstract class AbstractChaosContext implements ChaosContext {

  private volatile ConnectionSet connectionSet;
  private SerializerFactory serializerFactory;
  private Handler handler;
  private Protocol protocol;
  private ReentrantLock lock = new ReentrantLock();
  private Condition notFull = lock.newCondition();

  @Override
  public boolean isRunning() {
    return false;
  }

  /** Initialization the {@link ChaosContext}. */
  @Override
  public void start() {
    this.serializerFactory = SerializerFactory.getInstance();
    this.handler = HandlerRegister.INSTANCE.getHandler();
    this.protocol = new DefaultProtocol(this.serializerFactory);
  }

  /** Destroy the {@link ChaosContext}. avoid the resources wasted. */
  @Override
  public void destroy() {}

  /**
   * Returns the {@link SerializerFactory}.
   *
   * @return
   */
  @Override
  public SerializerFactory getSerializerFactory() {
    return this.serializerFactory;
  }

  /**
   * Returns the processor handler chain.
   *
   * @return
   */
  @Override
  public Handler getHandler() {
    return this.handler;
  }

  /**
   * Returns the {@link RecordAccumulator} Used for store request, scope Client. if you want get it
   * please instantiation client.
   *
   * <p>
   *
   * <p>ChaosClientContext clientContext = new ChaosClientContext()
   *
   * @return
   */
  @Override
  public RecordAccumulator getAccumulator() {
    return null;
  }

  @Override
  public Protocol getProtocol() {
    return this.protocol;
  }

  /**
   * Returns the {@link xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager} Used for
   * store request, scope Server. if you want get it please instantiation client.
   *
   * <p>
   *
   * <p>ChaosServerContext serverContext = new ChaosServerContext()
   *
   * @return
   */
  @Override
  public EventManager getEventManager() {
    return null;
  }

  @Override
  public void setEventManager(EventManager eventManager){

  }
  /**
   * Returns the heartbeat plugin. scope client.
   *
   * @return
   */
  @Override
  public Plugin getHeartbeatPlugin(long interval, ChaosContext context) {
    return new HeartBeatPlugin<Request>(interval, TimeUnit.MILLISECONDS) {

      @Override
      public void sendHeartRequest(Session session) throws IOException {
        //            LOGGER.info("Session : {} send heartbeat request.", session.getSessionId());
        ChaosRequest request = getRequestFactory().getRequest(HEARTBEAT);
        getProtocol().send0(request, session);
      }

      @Override
      public boolean isHeartBeatMessage(Session session, Request request) {
        if (request.getType() == HEARTBEAT.getType()) {
          getHandler().handler(session, request, context);
          return true;
        }
        return false;
      }
    };
  }

  /**
   * Returns the {@link xyz.vopen.framework.chaos.core.plugins.MonitorPlugin}.
   *
   * @return
   */
  @Override
  public NetMonitor getMonitor() {
    return new MonitorPlugin();
  }

  public ConnectionSet getConnectionSet() {
    return connectionSet;
  }

  public void setConnectionSet(ConnectionSet connectionSet) {
    this.connectionSet = connectionSet;
  }

  /**
   * Converts to a {@link ConnectionPair} collection with given parameter.
   *
   * @return List.
   */
  public List<ConnectionPair> convertConnectPair(String services, int port) {
    List<ConnectionPair> collect =
        Stream.of(services.split(","))
            .map(
                ser -> {
                  String[] split = ser.split(":");
                  return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
                })
            .filter(address -> address.getPort() != port)
            .map(address -> new ConnectionPair(address, -1))
            .collect(Collectors.toList());
    return collect;
  }

  @Override
  public void wakeupNotFull(){
    try {
      lock.lock();
      this.notFull.signal();
    } catch (Exception e) {
      e.printStackTrace();
    }finally{
      lock.unlock();
    }
  }

  @Override
  public Condition getNotFullCondition(){
    return this.notFull;
  }

  @Override
  public ReentrantLock getNotFullLock() {
    return this.lock;
  }

  /** Manager {@link ConnectionPair} base on {@link Deque}. */
  public class ConnectionSet {
    private Deque<ConnectionPair> queue;

    public ConnectionSet(List<ConnectionPair> list) {

      this.queue = new ArrayDeque<>(list);
    }

    /**
     * Retrieves, but does not remove, the first element of this deque, or returns {@code null} if
     * this deque is empty.
     *
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    public ConnectionPair peek() {
      if (this.queue.size() <= 0) {
        return null;
      }
      return this.queue.peekFirst();
    }

    /**
     * Retrieves and removes the first element of this deque, or returns {@code null} if this deque
     * is empty.
     *
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    public ConnectionPair poll() {
      if (this.queue.size() <= 0) {
        return null;
      }
      return this.queue.pollFirst();
    }

    /**
     * Retrieves and removes the first element of this deque. This method differs from {@link
     * Deque#pollFirst pollFirst} only in that it throws an exception if this deque is empty.
     *
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    public ConnectionPair remove() {
      if (this.queue.size() <= 0) {
        return null;
      }
      return this.queue.removeFirst();
    }

    /**
     * Removes the first occurrence of the specified element from this deque. If the deque does not
     * contain the element, it is unchanged. More formally, removes the first element {@code e} such
     * that <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt> (if such an element
     * exists). Returns {@code true} if this deque contained the specified element (or equivalently,
     * if this deque changed as a result of the call).
     *
     * <p>This method is equivalent to {@link Deque#removeFirstOccurrence(Object)}.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element is incompatible with this
     *     deque (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this deque does not permit
     *     null elements (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    public void remove(Object o) {
      if (this.queue.size() <= 0) {
        return;
      }
      this.queue.remove(o);
    }

    public void reset() {
      this.queue = new ArrayDeque<>();
    }

    /**
     * Inserts the specified element at the end of this deque if it is possible to do so immediately
     * without violating capacity restrictions, throwing an {@code IllegalStateException} if no
     * space is currently available. When using a capacity-restricted deque, it is generally
     * preferable to use method {@link Deque#offerLast}.
     *
     * <p>This method is equivalent to {@link Deque#add}.
     *
     * @throws IllegalStateException if the element cannot be added at this time due to capacity
     *     restrictions
     * @throws ClassCastException if the class of the specified element prevents it from being added
     *     to this deque
     * @throws NullPointerException if the specified element is null and this deque does not permit
     *     null elements
     * @throws IllegalArgumentException if some property of the specified element prevents it from
     *     being added to this deque
     */
    public void offer(ConnectionPair address) {
      this.queue.addLast(address);
    }

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    public int size() {
      return this.queue.size();
    }
  }

  /** The pair of connection. */
  public static class ConnectionPair {

    /** Connection address. */
    InetSocketAddress address;

    /** Represent connection times. */
    int retry;

    public ConnectionPair(InetSocketAddress address, int retry) {
      this.address = address;
      this.retry = retry;
    }

    public InetSocketAddress getAddress() {
      return address;
    }

    public int getRetry() {
      return retry;
    }

    public void setRetry(int retry) {
      this.retry = retry;
    }
  }
}
