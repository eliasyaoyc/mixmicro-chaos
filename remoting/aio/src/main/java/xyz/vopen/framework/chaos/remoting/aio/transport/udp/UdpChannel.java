package xyz.vopen.framework.chaos.remoting.aio.transport.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.remoting.aio.ChaosServerConfig;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPage;
import xyz.vopen.framework.chaos.remoting.aio.buffer.VirtualBuffer;
import xyz.vopen.framework.chaos.remoting.aio.transport.WriterBuffer;
import xyz.vopen.framework.chaos.remoting.api.AbstractAioSession;
import xyz.vopen.framework.chaos.remoting.api.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/**
 * {@link UdpChannel}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public final class UdpChannel {

  private static final Logger LOGGER = LoggerFactory.getLogger(UdpChannel.class);

  public ChaosServerConfig chaosServerConfig;
  private BufferPage bufferPage;

  /** real udp channel. */
  private DatagramChannel channel;

  private SelectionKey selectionKey;
  private Map<String, UdpAioSession> udpAioSessionConcurrentHashMap =
      new ConcurrentHashMap<>();

  private ConcurrentLinkedQueue<ResponseTask> responseTasks;
  private ResponseTask failWriteEvent;
  private Semaphore writeSemaphore = new Semaphore(1);

  public UdpChannel(
      ChaosServerConfig chaosServerConfig,
      BufferPage bufferPage,
      final DatagramChannel channel,
      SelectionKey selectionKey) {
    responseTasks = new ConcurrentLinkedQueue<>();
    this.chaosServerConfig = chaosServerConfig;
    this.bufferPage = bufferPage;
    this.channel = channel;
    this.selectionKey = selectionKey;
  }

  private void write(VirtualBuffer virtualBuffer, SocketAddress remote) throws IOException {
    if (writeSemaphore.tryAcquire()
        && responseTasks.isEmpty()
        && send(virtualBuffer.buffer(), remote) > 0) {
      virtualBuffer.clean();
      writeSemaphore.release();
      return;
    }
    responseTasks.offer(new ResponseTask(remote, virtualBuffer));
    if ((selectionKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
      selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
    }
  }

  protected void flush() throws IOException {
    while (true) {
      ResponseTask responseTask;
      if (failWriteEvent == null) {
        responseTask = responseTasks.poll();
        LOGGER.info("poll form writeBuffer :{}", responseTask);
      } else {
        responseTask = failWriteEvent;
        failWriteEvent = null;
      }
      if (responseTask == null) {
        writeSemaphore.release();
        if (responseTasks.isEmpty()) {
          selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
          if (!responseTasks.isEmpty()) {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
          }
        }
        return;
      }
      if (send(responseTask.response.buffer(), responseTask.remote) > 0) {
        responseTask.response.clean();
      } else {
        failWriteEvent = responseTask;
        break;
      }
    }
  }

  private int send(ByteBuffer byteBuffer, SocketAddress remote) throws IOException {
    AbstractAioSession aioSession = udpAioSessionConcurrentHashMap.get(getSessionKey(remote));
    if (chaosServerConfig.getMonitor() != null) {
      chaosServerConfig.getMonitor().beforeWrite(aioSession);
    }
    int size = channel.send(byteBuffer, remote);
    if (chaosServerConfig.getMonitor() != null) {
      chaosServerConfig.getMonitor().afterWrite(aioSession, size);
    }
    return size;
  }

  /**
   * establish a connection session with the remote service. data transfer via {@link
   * AbstractAioSession}.
   *
   * @param remote
   * @return
   */
  public AbstractAioSession connect(SocketAddress remote) {
    return createAndCacheSession(remote);
  }

  /**
   * creation and cache specified session.
   *
   * @return
   */
  protected UdpAioSession createAndCacheSession(final SocketAddress remote) {
    String key = getSessionKey(remote);
    UdpAioSession session = udpAioSessionConcurrentHashMap.get(key);
    if (session != null) {
      return session;
    }
    synchronized (this) {
      if (session != null) {
        return session;
      }
      Function<WriterBuffer, Void> function =
          writeBuffer -> {
            VirtualBuffer virtualBuffer = writeBuffer.poll();
            if (virtualBuffer == null) {
              return null;
            }
            try {
              write(virtualBuffer, remote);
            } catch (IOException e) {
              e.printStackTrace();
            }
            return null;
          };
      WriterBuffer writerBuffer =
          new WriterBuffer(bufferPage, function, chaosServerConfig.getWriteBufferSize(), 1);
      session = new UdpAioSession<>(this, remote, writerBuffer);
      udpAioSessionConcurrentHashMap.put(key, session);
    }
    return session;
  }

  private String getSessionKey(final SocketAddress remote) {
    if (!(remote instanceof InetSocketAddress)) {
      throw new UnsupportedOperationException();
    }
    InetSocketAddress address = (InetSocketAddress) remote;
    return address.getHostName() + ":" + address.getPort();
  }

  protected void removeSession(final SocketAddress remote) {
    String key = getSessionKey(remote);
    UdpAioSession udpAioSession = udpAioSessionConcurrentHashMap.remove(key);
    LOGGER.info("remove session : {}", udpAioSession);
  }

  public void close() {
    if (selectionKey != null) {
      Selector selector = selectionKey.selector();
      selectionKey.cancel();
      selector.wakeup();
      selectionKey = null; // help gc.
    }
    for (Map.Entry<String, UdpAioSession> entry :
        udpAioSessionConcurrentHashMap.entrySet()) {
      entry.getValue().close();
    }
    try {
      if (channel != null) {
        channel.close();
        channel = null;
      }
    } catch (IOException e) {
      LOGGER.error("", e);
    }
    ResponseTask task;
    while ((task = responseTasks.poll()) != null) {
      task.response.clean();
    }
  }

  protected DatagramChannel getChannel() {
    return channel;
  }

  final class ResponseTask {
    private SocketAddress remote;
    private VirtualBuffer response;

    public ResponseTask(SocketAddress remote, VirtualBuffer response) {
      this.remote = remote;
      this.response = response;
    }
  }
}
