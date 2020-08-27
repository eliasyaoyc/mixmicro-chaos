package xyz.vopen.framework.chaos.common.spi;

/**
 * {@link Wrapper} The annotated class will only work as a wrapper when the condition matches.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
public @interface Wrapper {

  /** the extension names that need to be wrapped. */
  String[] matches() default {};

  /** the extension names that need to excluded. */
  String[] misMatches() default {};
}
