package xyz.vopen.framework.chaos.core.internals;

/**
 * {@link AttributeMap} Holds this which can be accessed via {@link AttributeKey} Implementations
 * must be Thread-safe.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/20
 */
public interface AttributeMap {

  /**
   * Get the {@link Attribute} for the given {@link AttributeKey}. This method will never return
   * null, but may return an {@link Attribute} which dose not have a value set yet.
   */
  <T> Attribute<T> attr(AttributeKey<T> key);

  /**
   * Returns {@code true} if and only if the given {@link Attribute} exists in this {@link
   * AttributeMap}
   */
  <T> boolean hasAttr(AttributeKey<T> key);
}
