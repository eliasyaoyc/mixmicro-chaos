package xyz.vopen.framework.chaos.common;

import java.net.URL;

/**
 * {@link LifeCycle} Represents the life cycle of the entire service, e.g start,destroy.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
public interface LifeCycle {

  /**
   * true if running otherwise false.
   *
   * @return
   */
  boolean isRunning();

  /**
   * Root method that start server.
   */
  void start();

  /**
   * Root method that destroy sever.
   */
  void destroy();
}
