package xyz.vopen.framework.chaos.common.spi;

/**
 * {@link ExtensionFactory}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
@SPI
public interface ExtensionFactory {

  /**
   * Return extension.
   *
   * @param type the type of object.
   * @param name the name of object.
   * @return object instance.
   */
  <T> T getExtension(Class<T> type, String name);
}
