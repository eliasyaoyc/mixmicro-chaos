package xyz.vopen.framework.chaos.remoting.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.common.utilities.IOUtility;
import xyz.vopen.framework.chaos.common.utilities.ThreadFactoryUtility;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferFactory;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPagePool;
import xyz.vopen.framework.chaos.remoting.aio.transport.ConcurrentReadCompletionHandler;
import xyz.vopen.framework.chaos.remoting.aio.transport.ReadCompletionHandler;
import xyz.vopen.framework.chaos.remoting.aio.transport.TcpAioSession;
import xyz.vopen.framework.chaos.remoting.aio.transport.WriteCompletionHandler;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * {@link ChaosServer}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
public final class ChaosServer<T> extends AbstractChaosServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChaosServer.class);

  private final ChaosServerConfig chaosServerConfig;

  /** memory pool. */
  private BufferPagePool bufferPool;

  /** read callback event handing */
  private ReadCompletionHandler<T> aioReadCompletionHandler;

  /** write callback event handing */
  private WriteCompletionHandler<T> aioWriteCompletionHandler;

  /** inner memory pool */
  private BufferPagePool innerBufferPool = null;

  /** the function that connection session instantiation. */
  private Function<AsynchronousSocketChannel, TcpAioSession<T>> aioSessionFunction;

  /** asynchronousServerSocketChannel */
  private AsynchronousServerSocketChannel serverSocketChannel = null;

  /** asynchronousChannelGroup */
  private AsynchronousChannelGroup asynchronousChannelGroup;

  /** construct {@link ChaosServer} */
  public ChaosServer(ChaosServerConfig chaosServerConfig) {
    this.chaosServerConfig = chaosServerConfig;
  }

  /** Initialization inner components. */
  @Override
  public void preInit() {
  }

  @Override
  public void start() {
    super.start();
    LOGGER.info(
        "chaos server starting, host :{} port :{}",
        chaosServerConfig.getHost(),
        chaosServerConfig.getPort());
    try {
      start0(
          channel ->
              new TcpAioSession<T>(
                  channel,
                  aioWriteCompletionHandler,
                  bufferPool.allocateBufferPage(),
                  aioReadCompletionHandler,
                  chaosServerConfig));

    } catch (IOException e) {
      LOGGER.error("chaos server start failure, cause :{}", e.getCause());
      destroy();
    }
  }

  /**
   * the method that inner start.
   *
   * @param aioSessionFunction
   * @throws IOException
   */
  private final void start0(
      Function<AsynchronousSocketChannel, TcpAioSession<T>> aioSessionFunction) throws IOException {
    checkAndResetConfig();
    try {
      aioWriteCompletionHandler = new WriteCompletionHandler<>();
      if (bufferPool == null) {
        this.bufferPool = chaosServerConfig.getBufferFactory().create();
        this.innerBufferPool = bufferPool;
      }
      this.aioSessionFunction = aioSessionFunction;

      aioReadCompletionHandler =
          new ConcurrentReadCompletionHandler<>(
              new Semaphore(chaosServerConfig.getThreadNum() - 1));
      asynchronousChannelGroup =
          AsynchronousChannelGroup.withFixedThreadPool(
              chaosServerConfig.getThreadNum(), ThreadFactoryUtility.createFactory("chaos-server"));
      this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);

      // set socket options.
      if (chaosServerConfig.getSocketOptions() != null) {
        Map<SocketOption<Object>, Object> socketOptions = chaosServerConfig.getSocketOptions();
        for (Map.Entry<SocketOption<Object>, Object> entry : socketOptions.entrySet()) {
          this.serverSocketChannel.setOption(entry.getKey(), entry.getValue());
        }
      }
      // bind host.
      if (chaosServerConfig.getHost() != null) {
        serverSocketChannel.bind(
            new InetSocketAddress(chaosServerConfig.getHost(), chaosServerConfig.getPort()),
            chaosServerConfig.getBacklog());
      } else {
        serverSocketChannel.bind(
            new InetSocketAddress(chaosServerConfig.getPort()), chaosServerConfig.getBacklog());
      }
      startAcceptThread();

    } catch (Exception e) {
      destroy();
      throw e;
    }
  }

  /** start the accept thread used for process connection. */
  private void startAcceptThread() {
    serverSocketChannel.accept(
        null,
        new CompletionHandler<AsynchronousSocketChannel, Void>() {
          @Override
          public void completed(AsynchronousSocketChannel channel, Void attachment) {
            try {
              serverSocketChannel.accept(attachment, this);
            } catch (Exception e) {
              chaosServerConfig
                  .getProcessor()
                  .stateEvent(null, StateMachineEnum.ACCEPT_EXCEPTION, e);
              failed(e, attachment);
              serverSocketChannel.accept(attachment, this);
            }
            createSession(channel);
          }

          @Override
          public void failed(Throwable exc, Void attachment) {
            exc.printStackTrace();
          }
        });
  }

  private void checkAndResetConfig() {
    if (chaosServerConfig.getThreadNum() == 1) {
      chaosServerConfig.setThreadNum(2);
    }
  }

  private void createSession(AsynchronousSocketChannel channel) {
    TcpAioSession<T> session = null;
    AsynchronousSocketChannel acceptChannel = channel;
    try {
      if (chaosServerConfig.getMonitor() != null) {
        acceptChannel = chaosServerConfig.getMonitor().shouldAccept(channel);
      }
      if (acceptChannel != null) {
        session = aioSessionFunction.apply(acceptChannel);
        session.initSession();
      } else {
        chaosServerConfig.getProcessor().stateEvent(null, StateMachineEnum.REJECT_ACCEPT, null);
        IOUtility.close(channel);
      }
    } catch (Exception e) {
      LOGGER.error("create session encounter exception : {} ", e.getMessage());
      if (session == null) {
        IOUtility.close(channel);
      } else {
        session.close();
      }
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    try {
      if (serverSocketChannel != null) {
        serverSocketChannel.close();
        serverSocketChannel = null; // help gc.
      }
    } catch (IOException e) {
      LOGGER.error(
          "close chaos server [ AsynchronousServerSocketChannel ] occur exception : {}",
          e.getMessage());
    }
    if (!asynchronousChannelGroup.isTerminated() && !asynchronousChannelGroup.isShutdown()) {
      try {
        asynchronousChannelGroup.shutdown();
      } catch (Exception e) {
        LOGGER.error(
            "close chaos server [ AsynchronousChannelGroup ] occur exception : {}", e.getMessage());
      }
    }
    try {
      asynchronousChannelGroup.awaitTermination(3, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.error(
          "await chaos server [ AsynchronousChannelGroup ] occur exception : {}", e.getMessage());
    }
    if (innerBufferPool != null) {
      innerBufferPool.release();
    }
    aioReadCompletionHandler.shutdown();
  }

  public final ChaosServer<T> setReadBufferSize(int size) {
    this.chaosServerConfig.setReadBufferSize(size);
    return this;
  }

  /**
   * 设置Socket的TCP参数配置。
   *
   * <p>AIO客户端的有效可选范围为：<br>
   * 2. StandardSocketOptions.SO_RCVBUF<br>
   * 4. StandardSocketOptions.SO_REUSEADDR<br>
   *
   * @param socketOption 配置项
   * @param value 配置值
   * @param <V> 配置项类型
   * @return 当前AioQuickServer对象
   */
  public final <V> ChaosServer<T> setOption(SocketOption<V> socketOption, V value) {
    this.chaosServerConfig.setOption(socketOption, value);
    return this;
  }

  /**
   * 设置服务工作线程数,设置数值必须大于等于2
   *
   * @param threadNum 线程数
   * @return 当前AioQuickServer对象
   */
  public final ChaosServer<T> setThreadNum(int threadNum) {
    if (threadNum <= 1) {
      throw new InvalidParameterException("threadNum must >= 2");
    }
    this.chaosServerConfig.setThreadNum(threadNum);
    return this;
  }

  /**
   * 设置输出缓冲区容量
   *
   * @param bufferSize 单个内存块大小
   * @param bufferCapacity 内存块数量上限
   * @return 当前AioQuickServer对象
   */
  public final ChaosServer<T> setWriteBuffer(int bufferSize, int bufferCapacity) {
    this.chaosServerConfig.setWriteBufferSize(bufferSize);
    this.chaosServerConfig.setWriteBufferCapacity(bufferCapacity);
    return this;
  }

  /**
   * 设置 backlog 大小
   *
   * @param backlog backlog大小
   * @return 当前AioQuickServer对象
   */
  public final ChaosServer<T> setBacklog(int backlog) {
    this.chaosServerConfig.setBacklog(backlog);
    return this;
  }

  /**
   * 设置内存池。 通过该方法设置的内存池，在AioQuickServer执行shutdown时不会触发内存池的释放。
   * 该方法适用于多个AioQuickServer、AioQuickClient共享内存池的场景。 <b>在启用内存池的情况下会有更好的性能表现</b>
   *
   * @param bufferPool 内存池对象
   * @return 当前AioQuickServer对象
   */
  public final ChaosServer<T> setBufferPagePool(BufferPagePool bufferPool) {
    this.bufferPool = bufferPool;
    this.chaosServerConfig.setBufferFactory(BufferFactory.DISABLED_BUFFER_FACTORY);
    return this;
  }

  /**
   * 设置内存池的构造工厂。 通过工厂形式生成的内存池会强绑定到当前AioQuickServer对象， 在AioQuickServer执行shutdown时会释放内存池。
   * <b>在启用内存池的情况下会有更好的性能表现</b>
   *
   * @param bufferFactory 内存池工厂
   * @return 当前AioQuickServer对象
   */
  public final ChaosServer<T> setBufferFactory(BufferFactory bufferFactory) {
    this.chaosServerConfig.setBufferFactory(bufferFactory);
    this.bufferPool = null;
    return this;
  }
}
