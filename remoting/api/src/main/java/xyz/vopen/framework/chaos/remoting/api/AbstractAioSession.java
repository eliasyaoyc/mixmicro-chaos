package xyz.vopen.framework.chaos.remoting.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;

/**
 * {@link AbstractAioSession}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public abstract class AbstractAioSession implements Session {

  private static final String SESSION_ID = "aioSession-";

  /** currently session status. */
  protected byte status = SESSION_STATUS_ENABLED;

  private Object attachment;

  public AbstractAioSession() {}

  @Override
  public void close() {
    close(true);
  }

  @Override
  public String getSessionId() {
    return null;
  }

  @Override
  public boolean isInvalid() {
    return false;
  }

  @Override
  public <T1> T1 getAttachment() {
    return null;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getInputStream(int length) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T1> void setAttachment() {}

  public void close(boolean immediate){

  }

  public Buffer writeBuffer(){
    return null;
  }

  public abstract InetSocketAddress getLocalAddress() throws IOException;

  protected void assertChannel(Channel channel) throws IOException {
    if (status == SESSION_STATUS_CLOSED || channel == null) {
      throw new IOException("session is closed");
    }
  }
}
