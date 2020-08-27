package xyz.vopen.framework.chaos.remoting.aio.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.remoting.api.NetMonitor;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

import java.nio.channels.CompletionHandler;

/**
 * {@link WriteCompletionHandler} the callback for written event.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class WriteCompletionHandler<T> implements CompletionHandler<Integer, TcpAioSession<T>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(WriteCompletionHandler.class);

  @Override
  public void completed(final Integer result, final TcpAioSession<T> tcpAioSession) {
    try {
      NetMonitor<T> monitor = tcpAioSession.getServerConfig().getMonitor();
      if (monitor != null) {
        monitor.afterWrite(tcpAioSession, result);
      }
      tcpAioSession.writeCompleted();
    } catch (Exception e) {
      failed(e, tcpAioSession);
    }
  }

  @Override
  public void failed(Throwable exc, TcpAioSession<T> tcpAioSession) {
    LOGGER.warn("[ WriteCompletionHandler ] occur error : {}",exc.getMessage(),exc);
    try {
      tcpAioSession
          .getServerConfig()
          .getProcessor()
          .stateEvent(tcpAioSession, StateMachineEnum.OUTPUT_EXCEPTION, exc);
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      tcpAioSession.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
