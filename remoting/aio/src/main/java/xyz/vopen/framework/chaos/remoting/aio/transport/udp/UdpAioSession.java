package xyz.vopen.framework.chaos.remoting.aio.transport.udp;

import xyz.vopen.framework.chaos.remoting.aio.transport.WriterBuffer;
import xyz.vopen.framework.chaos.remoting.api.AbstractAioSession;
import xyz.vopen.framework.chaos.remoting.api.Buffer;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * {@link UdpAioSession}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class UdpAioSession<T> extends AbstractAioSession {

  private UdpChannel udpChannel;

  private SocketAddress remote;

  private WriterBuffer writerBuffer;

  public UdpAioSession(UdpChannel udpChannel, SocketAddress remote, WriterBuffer writerBuffer) {
    this.udpChannel = udpChannel;
    this.remote = remote;
    this.writerBuffer = writerBuffer;
    udpChannel
        .chaosServerConfig
        .getProcessor()
        .stateEvent(this, StateMachineEnum.NEW_SESSION, null);
  }

  @Override
  public void close(boolean immediate) {
    writerBuffer.close();
    udpChannel
            .chaosServerConfig
            .getProcessor()
            .stateEvent(this, StateMachineEnum.SESSION_CLOSED, null);
    udpChannel.removeSession(remote);
  }

  @Override
  public Buffer writeBuffer() {
    return this.writerBuffer;
  }

  @Override
  public void setSessionId(String sessionId) {

  }

  @Override
  public InetSocketAddress getLocalAddress() throws IOException {
    return (InetSocketAddress) udpChannel.getChannel().getLocalAddress();
  }

  @Override
  public InetSocketAddress getRemoteAddress()  {
    try {
      return (InetSocketAddress) udpChannel.getChannel().getRemoteAddress();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
