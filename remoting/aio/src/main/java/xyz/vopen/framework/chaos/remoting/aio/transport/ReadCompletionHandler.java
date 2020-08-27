package xyz.vopen.framework.chaos.remoting.aio.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.remoting.api.NetMonitor;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

import java.nio.channels.CompletionHandler;

/**
 * {@link ReadCompletionHandler} the callback for readable event.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class ReadCompletionHandler<T> implements CompletionHandler<Integer, TcpAioSession<T>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReadCompletionHandler.class);

  @Override
  public void completed(Integer result, TcpAioSession<T> tcpAioSession) {
    try {
      NetMonitor<T> monitor = tcpAioSession.getServerConfig().getMonitor();
      if (monitor != null) {
        monitor.afterRead(tcpAioSession, result);
      }
      tcpAioSession.readCompleted(result == -1);
    } catch (Exception e) {
      failed(e, tcpAioSession);
    }
  }

  @Override
  public void failed(Throwable exc, TcpAioSession<T> tcpAioSession) {
    LOGGER.warn("[ ReadCompletionHandler ] occur error : {}",exc.getMessage(),exc);
    try {
      tcpAioSession
          .getServerConfig()
          .getProcessor()
          .stateEvent(tcpAioSession, StateMachineEnum.INPUT_EXCEPTION, exc);
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      tcpAioSession.close(false);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void shutdown() {}
}
