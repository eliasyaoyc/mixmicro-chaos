package xyz.vopen.framework.chaos.remoting.aio.transport.udp;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * {@link UdpNullSelectionKey}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class UdpNullSelectionKey extends SelectionKey {

  @Override
  public SelectableChannel channel() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Selector selector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isValid() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void cancel() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int interestOps() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SelectionKey interestOps(int ops) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int readyOps() {
    throw new UnsupportedOperationException();
  }
}
