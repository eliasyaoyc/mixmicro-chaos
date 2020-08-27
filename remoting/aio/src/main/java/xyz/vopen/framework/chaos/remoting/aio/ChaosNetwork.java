package xyz.vopen.framework.chaos.remoting.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.common.exception.ChaosException;
import xyz.vopen.framework.chaos.common.utilities.ThreadFactoryUtility;
import xyz.vopen.framework.chaos.common.utilities.TimerTaskUtility;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferFactory;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPagePool;
import xyz.vopen.framework.chaos.remoting.aio.exception.ChaosNetworkException;
import xyz.vopen.framework.chaos.remoting.aio.transport.ReadCompletionHandler;
import xyz.vopen.framework.chaos.remoting.aio.transport.TcpAioSession;
import xyz.vopen.framework.chaos.remoting.aio.transport.WriteCompletionHandler;
import xyz.vopen.framework.chaos.remoting.aio.transport.WriterBuffer;
import xyz.vopen.framework.chaos.remoting.api.Buffer;
import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.Plugin;
import xyz.vopen.framework.chaos.remoting.api.Protocol;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.exception.RemotingException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static xyz.vopen.framework.chaos.common.utilities.TimerTaskUtility.cancelQuickTask;

/**
 * {@link ChaosNetwork}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
public class ChaosNetwork<T> extends AbstractChaosServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChaosNetwork.class);

  private static final String CHAOS_NETWORK = "Chaos-network";

  private ChaosServerConfig chaosConfig;
  private SocketAddress addresses;
  private Protocol protocol;
  private List<Session> sessions;
  private AsynchronousSocketChannel socketChannel;
  private Map<InetSocketAddress, Session> sessionMap = new HashMap<>();

  public ChaosNetwork(ChaosServerConfig chaosConfig) {
    this.chaosConfig = chaosConfig;
    this.protocol = this.chaosConfig.getProtocol();
  }

  private BufferPagePool bufferPagePool = null;

  private BufferPagePool innerBufferPool = null;

  private AsynchronousChannelGroup asynchronousChannelGroup;

  private SocketAddress localAddress;

  private int connectTimeout = 5000;

  @Override
  public void preInit() {}

  @Override
  public void start() {
    super.start();
    try {
      this.asynchronousChannelGroup =
          AsynchronousChannelGroup.withFixedThreadPool(
              2, ThreadFactoryUtility.createFactory(CHAOS_NETWORK));
      start0(asynchronousChannelGroup);
    } catch (IOException e) {
      LOGGER.error("chaos client start occur exception : {}", e.getMessage(), e);
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    shutdown0(false);
  }

  @Override
  public Session connect(InetSocketAddress address) {
    Session sess = null;
    if ((sess = this.sessionMap.get(address)) != null) {
      if (sess.isInvalid()) {
        this.sessionMap.remove(address);
        return null;
      }
      return sess;
    }

    try {
      this.asynchronousChannelGroup =
          AsynchronousChannelGroup.withFixedThreadPool(
              2, ThreadFactoryUtility.createFactory(CHAOS_NETWORK));
      socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
      if (bufferPagePool == null) {
        bufferPagePool = this.chaosConfig.getBufferFactory().create();
        this.innerBufferPool = bufferPagePool;
      }
      // set socket options.
      if (this.chaosConfig.getSocketOptions() != null) {
        Map<SocketOption<Object>, Object> socketOptions = this.chaosConfig.getSocketOptions();
        for (Map.Entry<SocketOption<Object>, Object> entry : socketOptions.entrySet()) {
          socketChannel.setOption(entry.getKey(), entry.getValue());
        }
      }
      // bind.
      Future<Void> future = socketChannel.connect(address);

      if (this.connectTimeout > 0) {
        future.get(connectTimeout, TimeUnit.MILLISECONDS);
      } else {
        future.get();
      }
      AsynchronousSocketChannel connectedChannel = socketChannel;
      if (this.chaosConfig.getMonitor() != null) {
        connectedChannel = this.chaosConfig.getMonitor().shouldAccept(socketChannel);
      }
      if (connectedChannel == null) {
        throw new RemotingException("Monitor refuse channel.");
      }
      TcpAioSession<T> session =
          new TcpAioSession<T>(
              connectedChannel,
              new WriteCompletionHandler<T>(),
              bufferPagePool.allocateBufferPage(),
              new ReadCompletionHandler<T>(),
              this.chaosConfig);
      session.initSession();

      LOGGER.info(
          "Chaos client connect success host : {}  port : {}",
          address.getHostName(),
          address.getPort());

      return session;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public boolean send(Request request, Session session) {
    if (this.protocol == null) {
      throw new NullPointerException("Protocol must be empty.");
    }
    return this.protocol.send0(request, session);
  }

  public void start0(AsynchronousChannelGroup asynchronousChannelGroup) {
    try {
      socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
      if (bufferPagePool == null) {
        bufferPagePool = this.chaosConfig.getBufferFactory().create();
        this.innerBufferPool = bufferPagePool;
      }
      // set socket options.
      if (this.chaosConfig.getSocketOptions() != null) {
        Map<SocketOption<Object>, Object> socketOptions = this.chaosConfig.getSocketOptions();
        for (Map.Entry<SocketOption<Object>, Object> entry : socketOptions.entrySet()) {
          socketChannel.setOption(entry.getKey(), entry.getValue());
        }
      }
      // bind node.
      Session sessionList = bind(this.addresses);
      if (sessionList == null) {
        throw new ChaosNetworkException(" Chaos network occur error. ");
      }
    } catch (Exception e) {
      throw new ChaosException(e);
    }
  }

  private Session bind(SocketAddress socketAddress) {

    try {
      // bind host.
      if (localAddress != null) {
        socketChannel.bind(localAddress);
      }

      Future<Void> future = socketChannel.connect(socketAddress);

      if (this.connectTimeout > 0) {
        future.get(connectTimeout, TimeUnit.MILLISECONDS);
      } else {
        future.get();
      }
      AsynchronousSocketChannel connectedChannel = socketChannel;
      if (this.chaosConfig.getMonitor() != null) {
        connectedChannel = this.chaosConfig.getMonitor().shouldAccept(socketChannel);
      }
      if (connectedChannel == null) {
        throw new RemotingException("Monitor refuse channel.");
      }
      TcpAioSession<T> session =
          new TcpAioSession<T>(
              connectedChannel,
              new WriteCompletionHandler<T>(),
              bufferPagePool.allocateBufferPage(),
              new ReadCompletionHandler<T>(),
              this.chaosConfig);
      session.initSession();
      return session;
    } catch (Exception e) {
      throw new ChaosException(e);
    }
  }

  /**
   * After chaosClient start that get the corresponding session and set them to {@link
   * ChaosNetwork#sessions}.
   */
  public void setSessionMap() {}

  public List<Session> getSession() {
    return this.sessions;
  }

  public Session getSessionForChaos() {
    return this.sessions.get(0);
  }

  /**
   * Returns the session via the given address.
   *
   * @param address
   * @return session.
   */
  public Session getSessionMap(InetSocketAddress address) {
    if (this.sessionMap != null) {
      return this.sessionMap.get(address);
    }
    return null;
  }

  public boolean isWakeup() {
    if (this.sessionMap != null) {
      return true;
    }
    return false;
  }

  /**
   * Add session.
   *
   * @param peer InetSocketAddress
   * @param session
   */
  public void putSession(InetSocketAddress peer, Session session) {
    this.sessionMap.put(peer, session);
  }

  public void destroyNow() {
    super.destroy();
    shutdown0(true);
  }

  private void shutdown0(boolean immediate) {
    if (sessions != null && sessions.size() > 0) {
      Iterator<Session> iterator = sessions.iterator();
      if (iterator.hasNext()) {
        Session next = iterator.next();
        next.close(immediate); // release buffer.
        iterator.remove();
      }
    }
    if (asynchronousChannelGroup != null) {
      asynchronousChannelGroup.shutdown();
    }
    if (innerBufferPool != null) {
      innerBufferPool.release();
    }
  }

  /**
   * Set readBuffer size.
   *
   * @param size unit byte.
   */
  public final ChaosNetwork<T> setReadBufferSize(int size) {
    this.chaosConfig.setReadBufferSize(size);
    return this;
  }

  /**
   * Set Socket TCP parameter configuration
   *
   * <p>1. StandardSocketOptions.SO_SNDBUF<br>
   * 2. StandardSocketOptions.SO_RCVBUF<br>
   * 3. StandardSocketOptions.SO_KEEPALIVE<br>
   * 4. StandardSocketOptions.SO_REUSEADDR<br>
   * 5. StandardSocketOptions.TCP_NODELAY
   */
  public final <V> ChaosNetwork<T> setOption(SocketOption<V> socketOption, V value) {
    this.chaosConfig.setOption(socketOption, value);
    return this;
  }

  /**
   * Bind the local address and port to connect to the remote service
   *
   * @param local If null is passed, the system automatically obtains it
   * @param port If passed 0 is specified by the system
   */
  public final ChaosNetwork<T> bindLocal(String local, int port) {
    this.localAddress =
        local == null ? new InetSocketAddress(port) : new InetSocketAddress(local, port);
    return this;
  }

  /**
   * Set the memory pool. The memory pool set by this method dose not trigger the release of the
   * memory pool when the {@link ChaosNetwork} executes the shutdown.This method is applicable to
   * the scenario of multiple AioQuickServer and AioQuickClient Shared memory pool. performs better
   * with memory pool enabled.
   *
   * @param bufferPool
   * @return ChaosClient object.
   */
  public final ChaosNetwork<T> setBufferPagePool(BufferPagePool bufferPool) {
    this.bufferPagePool = bufferPool;
    this.chaosConfig.setBufferFactory(BufferFactory.DISABLED_BUFFER_FACTORY);
    return this;
  }

  /**
   * Set the construction factory for the memory pool.The memory pool generated in factory form is
   * strongly bound to the current {@link ChaosNetwork} object, which is released when the {@link
   * ChaosNetwork} executed {@link ChaosNetwork#shutdown0(boolean)} and performs better with the
   * memory pool enabled.
   *
   * @param bufferFactory
   * @return ChaosClient object.
   */
  public final ChaosNetwork<T> setBufferFactory(BufferFactory bufferFactory) {
    this.chaosConfig.setBufferFactory(bufferFactory);
    this.bufferPagePool = null;
    return this;
  }

  /**
   * Set the output buffer capacity.
   *
   * @param bufferSize single memory block size.
   * @param bufferCapacity maximum number of memory blocks.
   * @return ChaosClient object.
   */
  public final ChaosNetwork<T> setWriteBuffer(int bufferSize, int bufferCapacity) {
    this.chaosConfig.setWriteBufferSize(bufferSize);
    this.chaosConfig.setWriteBufferCapacity(bufferCapacity);
    return this;
  }

  /**
   * Set Client connection timeout in milliseconds.
   *
   * @param timeout
   * @return
   */
  public final ChaosNetwork<T> connectTimeout(int timeout) {
    this.connectTimeout = timeout;
    return this;
  }
}
