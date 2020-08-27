package xyz.vopen.framework.chaos.core.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.common.utilities.TimerTaskUtility;
import xyz.vopen.framework.chaos.remoting.aio.ChaosServer;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPage;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPagePool;
import xyz.vopen.framework.chaos.remoting.api.AbstractPlugin;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * {@link BufferPageMonitorPlugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class BufferPageMonitorPlugin<T> extends AbstractPlugin<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BufferPageMonitorPlugin.class);

  private int seconds = 0;

  private ChaosServer<T> server;

  private ScheduledFuture<?> future;

  public BufferPageMonitorPlugin(int seconds, ChaosServer<T> server) {
    this.seconds = seconds;
    this.server = server;
    init();
  }

  private void init() {
    long mills = TimeUnit.SECONDS.toMillis(seconds);
    future =
        TimerTaskUtility.scheduleAtFixedRate(
            () -> {
              if (server == null) {
                LOGGER.error("unKnow server or client need to monitor.");
                shutdown();
                return;
              }
              try {
                Field bufferPoolField = ChaosServer.class.getDeclaredField("bufferPool");
                bufferPoolField.setAccessible(true);
                BufferPagePool pagePool = (BufferPagePool) bufferPoolField.get(server);
                if (pagePool == null) {
                  LOGGER.error("server maybe has not started.");
                  shutdown();
                  return;
                }
                Field field = BufferPagePool.class.getDeclaredField("bufferPages");
                field.setAccessible(true);
                BufferPage[] pages = (BufferPage[]) field.get(pagePool);
                StringBuilder sb = new StringBuilder();
                for (BufferPage page : pages) {
                  sb.append("\r\n" + page.toString());
                }
                LOGGER.info(sb.toString());
              } catch (Exception e) {
                LOGGER.error("", e);
              }
            },
            mills,
            mills);
  }

  private void shutdown() {
    if (future != null) {
      future.cancel(true);
      future = null; // help gc.
    }
  }
}
