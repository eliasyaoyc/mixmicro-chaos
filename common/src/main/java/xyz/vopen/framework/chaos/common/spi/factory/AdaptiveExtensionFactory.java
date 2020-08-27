package xyz.vopen.framework.chaos.common.spi.factory;

import xyz.vopen.framework.chaos.common.spi.Adaptive;
import xyz.vopen.framework.chaos.common.spi.ExtensionFactory;
import xyz.vopen.framework.chaos.common.spi.ExtensionLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link AdaptiveExtensionFactory}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
@Adaptive
public class AdaptiveExtensionFactory implements ExtensionFactory {

  public AdaptiveExtensionFactory() {}

  @Override
  public <T> T getExtension(Class<T> type, String name) {
    return null;
  }
}
