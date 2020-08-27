package xyz.vopen.framework.chaos.remoting.aio;

import xyz.vopen.framework.chaos.common.LifeCycle;
import xyz.vopen.framework.chaos.common.exception.ChaosException;
import xyz.vopen.framework.chaos.remoting.api.Session;

import java.net.InetSocketAddress;
import java.net.URL;

/**
 * {@link AbstractChaosServer}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
public abstract class AbstractChaosServer implements LifeCycle {

  private volatile boolean isRunning = false;

  public abstract void preInit();

  @Override
  public boolean isRunning() {
    return  isRunning;
  }

  @Override
  public void start() {
    if (isRunning) {
      throw new ChaosException("already running...please check !");
    }
    isRunning = true;
  }

  @Override
  public void destroy() {
    if (!isRunning) {
      throw new ChaosException("already closed...please check !");
    }
    isRunning = false;
  }

  public Session connect(InetSocketAddress address){
    return null;
  }
}
