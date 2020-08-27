package xyz.vopen.framework.chaos.common.spi;

import xyz.vopen.framework.chaos.common.Prioritized;

/**
 * {@link LoadingStrategy}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
public interface LoadingStrategy extends Prioritized {

  String directory();

  default boolean preferExtensionClassLoader() {
    return false;
  }

  default String[] excludedPackages() {
    return null;
  }

  /**
   * Indicates current {@link LoadingStrategy} supports overriding other lower prioritized instances
   * or not.
   *
   * @return if supports, return <code>true</code>, or <code>false</code>
   */
  default boolean overridden() {
    return false;
  }
}
