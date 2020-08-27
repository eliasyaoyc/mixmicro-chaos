package xyz.vopen.framework.chaos.common.spi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;

/**
 * {@link Adaptive} Provide helpful information for {@link ExtensionLoader} to inject dependency
 * extension instance.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Adaptive {

  /**
   * Decide which target extension to be injected. The name of the target extension is decided by
   * the parameter passed in the URL, and the parameter names are given by this method.
   *
   * <p>If the specified parameters are not found from {@link URL}, then the default extension will
   * be used for dependency injection (specified in its interface's {@link SPI}).
   *
   * <p>For example, given <code>String[] {"key1", "key2"}</code>:
   *
   * <ol>
   *   <li>find parameter 'key1' in URL, use its value as the extension's name
   *   <li>try 'key2' for extension's name if 'key1' is not found (or its value is empty) in URL
   *   <li>use default extension if 'key2' doesn't exist either
   *   <li>otherwise, throw {@link IllegalStateException}
   * </ol>
   *
   * If the parameter names are empty, then a default parameter name is generated from interface's
   * class name with the rule: divide classname from capital char into several parts, and separate
   * the parts with dot '.', for example, for {@code org.apache.dubbo.xxx.YyyInvokerWrapper}, the
   * generated name is <code>String[] {"yyy.invoker.wrapper"}</code>.
   *
   * @return parameter names in URL
   */
  String[] value() default {};
}
