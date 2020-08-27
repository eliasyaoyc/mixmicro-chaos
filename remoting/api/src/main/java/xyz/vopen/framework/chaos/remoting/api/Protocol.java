package xyz.vopen.framework.chaos.remoting.api;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * {@link Protocol}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public interface Protocol<T> {

  /** do encode. */
  byte[] encode(final Request request, Session session);

  /** do decode. */
  T decode(final ByteBuffer readBuffer, Session session);

  boolean send0(Request request,Session session);
}
