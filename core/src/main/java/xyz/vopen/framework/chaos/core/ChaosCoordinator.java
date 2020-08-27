package xyz.vopen.framework.chaos.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.common.utilities.TimerTaskUtility;
import xyz.vopen.framework.chaos.core.internals.ChaosServerContext;
import xyz.vopen.framework.chaos.remoting.aio.AbstractChaosServer;
import xyz.vopen.framework.chaos.remoting.aio.ChaosServer;
import xyz.vopen.framework.chaos.remoting.aio.ChaosServerConfig;

/**
 * {@link ChaosCoordinator} This is a main class. start chaos server.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/14
 */
public class ChaosCoordinator extends AbstractChaosServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChaosCoordinator.class);

  private final ChaosServerConfig config;
  private ChaosServer chaosServer;
  private ChaosServerContext context;

  public ChaosCoordinator(ChaosServerConfig config) {
    this.context = new ChaosServerContext(config);
    this.config = (ChaosServerConfig) context.getConfig();
    this.chaosServer = new ChaosServer(config);
  }

  @Override
  public void preInit() {
    if (this.config == null) {
      throw new NullPointerException("Get Chaos config failure is null.");
    }
    this.context.start();
    this.chaosServer.preInit();
  }

  @Override
  public void start() {

    // additional shutdown hook.
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                new Runnable() {
                  @Override
                  public void run() {
                    shutdown();
                  }
                }));

    try {
      // start chaos server.
      if (this.chaosServer != null) {
        this.chaosServer.start();
      }
    } catch (Exception e) {
      LOGGER.error("Coordinator start occur failure ");
      shutdown();
    }
  }

  @Override
  public void destroy() {
    shutdown();
  }

  /**
   * Returns the {@link ChaosServerContext}.
   * @return
   */
  public ChaosServerContext getContext(){
    return this.context;
  }

  private void shutdown() {
    LOGGER.warn("shutdown start...");

    // shutdown executor pool.
    TimerTaskUtility.cancelQuickTask();

    try {
      if (this.chaosServer != null) {
        this.chaosServer.destroy();
      }
      if (this.context != null) {
        this.context.destroy();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
