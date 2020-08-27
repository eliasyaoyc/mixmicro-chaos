package xyz.vopen.framework.chaos.core.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.remoting.api.AbstractPlugin;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * {@link BlackListPlugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public final class BlackListPlugin<T> extends AbstractPlugin<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlackListPlugin.class);

  private ConcurrentLinkedQueue<BlackListRule> ipBlackList = new ConcurrentLinkedQueue<>();

  @Override
  public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
    InetSocketAddress inetSocketAddress = null;

    try {
      inetSocketAddress = (InetSocketAddress) channel.getRemoteAddress();
    } catch (Exception e) {
      LOGGER.error("get remote address error", e);
    }
    if (inetSocketAddress == null) {
      return channel;
    }
    for (BlackListRule blackListRule : ipBlackList) {
      if (!blackListRule.access(inetSocketAddress)) {
        return null;
      }
    }
    return channel;
  }

  public void addRule(BlackListRule rule) {
    this.ipBlackList.add(rule);
  }

  public void removeRule(BlackListRule rule) {
    this.ipBlackList.remove(rule);
  }

  public interface BlackListRule {

    boolean access(InetSocketAddress address);
  }
}
