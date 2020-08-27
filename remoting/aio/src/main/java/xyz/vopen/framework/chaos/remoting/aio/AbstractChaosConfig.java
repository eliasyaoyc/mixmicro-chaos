package xyz.vopen.framework.chaos.remoting.aio;

import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.NetMonitor;

/**
 * {@link AbstractChaosConfig}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/26
 */
public abstract class AbstractChaosConfig implements ChaosConfig {

  private NetMonitor monitor;

  public NetMonitor getMonitor() {
    return this.monitor;
  }

  public void setMonitor(MessageProcessor processor) {
    if (processor instanceof NetMonitor) {
      this.monitor = (NetMonitor) processor;
    }
    throw new IllegalArgumentException("Processor must be NetMonitor implementation class");
  }
}
