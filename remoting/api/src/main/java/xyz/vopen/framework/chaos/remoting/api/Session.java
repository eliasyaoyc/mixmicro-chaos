package xyz.vopen.framework.chaos.remoting.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 * {@link Session}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public interface Session {

  /** the status of the session is closed. */
  byte SESSION_STATUS_CLOSED = 1;

  /** the status of the session is closing. */
  byte SESSION_STATUS_CLOSING = 2;

  /** the status of the session is normal. */
  byte SESSION_STATUS_ENABLED = 3;

  void close();

  void close(boolean immediate);

  Buffer writeBuffer();

  String getSessionId();

  void setSessionId(String sessionId);

  boolean isInvalid();

  <T> T getAttachment();

  <T> void setAttachment();

  InputStream getInputStream() throws IOException;

  InputStream getInputStream(int length) throws IOException;

  public InetSocketAddress getRemoteAddress() ;
}
