//package xyz.vopen.framework.chaos.remoting.aio.transport.udp;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import xyz.vopen.framework.chaos.remoting.aio.ChaosServerConfig;
//import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPage;
//import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPagePool;
//import xyz.vopen.framework.chaos.remoting.aio.buffer.VirtualBuffer;
//import xyz.vopen.framework.chaos.remoting.aio.exception.DecoderException;
//import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
//import xyz.vopen.framework.chaos.remoting.api.Protocol;
//import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.SocketAddress;
//import java.nio.ByteBuffer;
//import java.nio.channels.DatagramChannel;
//import java.nio.channels.SelectionKey;
//import java.nio.channels.Selector;
//import java.util.Iterator;
//import java.util.Set;
//import java.util.concurrent.ArrayBlockingQueue;
//
///**
// * {@link UdpBootstrap}
// *
// * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
// * @version ${project.version}
// * @date 2020/7/10
// */
//public class UdpBootstrap<Request> {
//
//  /** logger */
//  private static final Logger LOGGER = LoggerFactory.getLogger(UdpBootstrap.class);
//
//  private static final int MAX_EVENT = 512;
//
//  private static final int MAX_READ_TIMES = 16;
//  /** 服务ID */
//  private static int UID;
//
//  private final SelectionKey NEED_TO_POLL = new UdpNullSelectionKey();
//  private final SelectionKey EXECUTE_TASK_OR_SHUTDOWN = new UdpNullSelectionKey();
//  /** 服务状态 */
//  private volatile Status status = Status.STATUS_INIT;
//  /** 多路复用器 */
//  private Selector selector;
//  /** 服务配置 */
//  private ChaosServerConfig<Request> config = new ChaosServerConfig<>();
//
//  private ArrayBlockingQueue<SelectionKey> selectionKeys = new ArrayBlockingQueue<>(MAX_EVENT);
//
//  private UdpDispatcher[] workerGroup;
//
//  /** 缓存页 */
//  private BufferPage bufferPage = new BufferPagePool(1024, 1, -1, true).allocateBufferPage();
//
//  public UdpBootstrap(Protocol<Request> protocol, MessageProcessor<Request> messageProcessor) {
//    config.setProtocol(protocol);
//    config.setProcessor(messageProcessor);
//  }
//
//  /**
//   * 开启一个UDP通道，端口号随机
//   *
//   * @return UDP通道
//   */
//  public UdpChannel<Request> open() throws IOException {
//    return open(0);
//  }
//
//  /**
//   * 开启一个UDP通道
//   *
//   * @param port 指定绑定端口号,为0则随机指定
//   */
//  public UdpChannel<Request> open(int port) throws IOException {
//    return open(null, port);
//  }
//
//  /**
//   * 开启一个UDP通道
//   *
//   * @param host 绑定本机地址
//   * @param port 指定绑定端口号,为0则随机指定
//   */
//  public UdpChannel<Request> open(String host, int port) throws IOException {
//    if (selector == null) {
//      synchronized (this) {
//        if (selector == null) {
//          selector = Selector.open();
//        }
//      }
//    }
//
//    DatagramChannel channel = DatagramChannel.open();
//    channel.configureBlocking(false);
//    if (port > 0) {
//      channel
//          .socket()
//          .bind(host == null ? new InetSocketAddress(port) : new InetSocketAddress(host, port));
//    }
//
//    if (status == Status.STATUS_RUNNING) {
//      selector.wakeup();
//    }
//    SelectionKey selectionKey = channel.register(selector, SelectionKey.OP_READ);
//    UdpChannel<Request> udpChannel =
//        new UdpChannel<Request>(config, bufferPage, channel, selectionKey);
//    selectionKey.attach(udpChannel);
//
//    // 启动线程服务
//    if (status == Status.STATUS_INIT) {
//      initThreadServer();
//    }
//    return udpChannel;
//  }
//
//  private synchronized void initThreadServer() {
//    if (status != Status.STATUS_INIT) {
//      return;
//    }
//    updateServiceStatus(Status.STATUS_RUNNING);
//    int uid = UdpBootstrap.UID++;
//
//    // 启动worker线程组
//    workerGroup = new UdpDispatcher[config.getThreadNum()];
//    for (int i = 0; i < config.getThreadNum(); i++) {
//      workerGroup[i] = new UdpDispatcher(config.getProcessor());
//      new Thread(workerGroup[i], "UDP-Worker-" + i).start();
//    }
//    // 启动Boss线程组
//    selectionKeys.offer(NEED_TO_POLL);
//    for (int i = 0; i < 2; i++) {
//      new Thread(
//              new Runnable() {
//                @Override
//                public void run() {
//                  // 读缓冲区
//                  VirtualBuffer readBuffer = bufferPage.allocate(config.getReadBufferSize());
//                  SelectionKey key;
//                  try {
//                    while (true) {
//                      try {
//                        key = selectionKeys.take();
//                        if (key == NEED_TO_POLL) {
//                          try {
//                            key = poll();
//                          } catch (IOException x) {
//                            x.printStackTrace();
//                            return;
//                          }
//                        }
//                      } catch (InterruptedException e) {
//                        LOGGER.info("InterruptedException", e);
//                        continue;
//                      }
//                      if (key == EXECUTE_TASK_OR_SHUTDOWN) {
//                        LOGGER.info("stop thread:{}" + Thread.currentThread());
//                        break;
//                      }
//
//                      UdpChannel<Request> udpChannel = (UdpChannel<Request>) key.attachment();
//                      if (!key.isValid()) {
//                        udpChannel.close();
//                        continue;
//                      }
//
//                      if (key.isReadable()) {
//                        try {
//                          doRead(readBuffer, udpChannel);
//                        } catch (Exception e) {
//                          e.printStackTrace();
//                        }
//                      }
//                      if (key.isWritable()) {
//                        try {
//                          udpChannel.flush();
//                        } catch (IOException e) {
//                          e.printStackTrace();
//                        }
//                      }
//                    }
//                  } finally {
//                    // 读缓冲区内存回收
//                    readBuffer.clean();
//                  }
//                }
//              },
//              "UDP-Boss-" + uid + "-" + i)
//          .start();
//    }
//  }
//
//  private void updateServiceStatus(final Status status) {
//    this.status = status;
//    //        notifyWhenUpdateStatus(status);
//  }
//
//  /**
//   * 获取待处理的Key
//   *
//   * @return
//   * @throws IOException
//   */
//  private SelectionKey poll() throws IOException {
//    try {
//      while (true) {
//        if (status != Status.STATUS_RUNNING) {
//          LOGGER.info("current status is :{}, will shutdown", status);
//          return EXECUTE_TASK_OR_SHUTDOWN;
//        }
//        Set<SelectionKey> selectionKeys = selector.selectedKeys();
//        if (selectionKeys.isEmpty()) {
//          selector.select();
//        }
//        if (status != Status.STATUS_RUNNING) {
//          LOGGER.info("current status is :{}, will shutdown", status);
//          return EXECUTE_TASK_OR_SHUTDOWN;
//        }
//        Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
//        int max = selectionKeys.size();
//        if (max > MAX_EVENT) {
//          max = MAX_EVENT;
//        }
//
//        while (max-- > 0) {
//          final SelectionKey key = keyIterator.next();
//          keyIterator.remove();
//          try {
//            if (max > 0) {
//              this.selectionKeys.offer(key);
//            } else {
//              return key;
//            }
//          } catch (Exception e) {
//            e.printStackTrace();
//          }
//        }
//      }
//    } finally {
//      selectionKeys.offer(NEED_TO_POLL);
//    }
//  }
//
//  /**
//   * 去读数据
//   *
//   * @param channel
//   * @throws IOException
//   */
//  private void doRead(VirtualBuffer readBuffer, UdpChannel channel) throws IOException {
//    int count = MAX_READ_TIMES;
//    while (count-- > 0) {
//      // 接收数据
//      ByteBuffer buffer = readBuffer.buffer();
//      buffer.clear();
//      // The datagram's source address,
//      // or null if this channel is in non-blocking mode and no datagram was immediately available
//      SocketAddress remote = channel.getChannel().receive(buffer);
//      if (remote == null) {
//        return;
//      }
//      buffer.flip();
//
//      UdpAioSession<Request> aioSession = channel.createAndCacheSession(remote);
//      config.getMonitor().beforeRead(aioSession);
//      config.getMonitor().afterRead(aioSession, buffer.remaining());
//      Request request = null;
//      // 解码
//      try {
//        request = config.getProtocol().decode(buffer, aioSession);
//      } catch (Exception e) {
//        config.getProcessor().stateEvent(aioSession, StateMachineEnum.DECODE_EXCEPTION, e);
//        aioSession.close();
//        throw e;
//      }
//      // 理论上每个UDP包都是一个完整的消息
//      if (request == null) {
//        config
//            .getProcessor()
//            .stateEvent(
//                aioSession,
//                StateMachineEnum.DECODE_EXCEPTION,
//                new DecoderException("decode result is null"));
//        return;
//      }
//
//      LOGGER.info("receive:{} from:{}", request, remote);
//
//      // 任务分发
//      int hashCode = remote.hashCode();
//      if (hashCode < 0) {
//        hashCode = -hashCode;
//      }
//      UdpDispatcher dispatcher = workerGroup[hashCode % workerGroup.length];
//      dispatcher.dispatch(aioSession, request);
//    }
//  }
//
//  public void shutdown() {
//    status = Status.STATUS_STOPPING;
//    selector.wakeup();
//
//    for (UdpDispatcher dispatcher : workerGroup) {
//      dispatcher.dispatch(dispatcher.EXECUTE_TASK_OR_SHUTDOWN);
//    }
//  }
//
//  /**
//   * 设置读缓存区大小
//   *
//   * @param size 单位：byte
//   */
//  public final UdpBootstrap<Request> setReadBufferSize(int size) {
//    this.config.setReadBufferSize(size);
//    return this;
//  }
//
//  /**
//   * 设置线程大小
//   *
//   * @param num
//   */
//  public final UdpBootstrap<Request> setThreadNum(int num) {
//    this.config.setThreadNum(num);
//    return this;
//  }
//
//  enum Status {
//    /** 状态：初始 */
//    STATUS_INIT,
//    /** 状态：初始 */
//    STATUS_STARTING,
//    /** 状态：运行中 */
//    STATUS_RUNNING,
//    /** 状态：停止中 */
//    STATUS_STOPPING,
//    /** 状态：已停止 */
//    STATUS_STOPPED;
//  }
//}
