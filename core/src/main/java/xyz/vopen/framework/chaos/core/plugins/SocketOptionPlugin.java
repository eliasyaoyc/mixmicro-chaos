package xyz.vopen.framework.chaos.core.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.remoting.api.AbstractPlugin;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link SocketOptionPlugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class SocketOptionPlugin<T> extends AbstractPlugin<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SocketOptionPlugin.class);

  private Map<SocketOption<Object>, Object> optionMap = new HashMap<>();

  @Override
  public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
    setOption(channel);
    return super.shouldAccept(channel);
  }

  public void setOption(AsynchronousSocketChannel channel) {
    try {
      if (!optionMap.containsKey(StandardSocketOptions.TCP_NODELAY)) {
        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
      }
      for (Map.Entry<SocketOption<Object>, Object> entry : optionMap.entrySet()) {
        channel.setOption(entry.getKey(), entry.getValue());
      }
    } catch (IOException e) {
      LOGGER.error("", e);
    }
  }

  public final <V> SocketOptionPlugin<T> setOption(SocketOption<V> socketOption, V value) {
    put0(socketOption, value);
    return this;
  }

  public void put0(SocketOption socketOption, Object value) {
    optionMap.put(socketOption, value);
  }
}
