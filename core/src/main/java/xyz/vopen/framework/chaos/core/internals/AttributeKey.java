package xyz.vopen.framework.chaos.core.internals;

/**
 * {@link AttributeKey} Key which can be used to access {@link Attribute} out of the {@link
 * AttributeMap}. Be aware that it is not be possible to have multiple keys with the same name.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/20
 */
public class AttributeKey<T> extends AbstractConstant<AttributeKey<T>> {

  private static final ConstantPool<AttributeKey<Object>> pool =
      new ConstantPool<AttributeKey<Object>>() {
        @Override
        protected AttributeKey<Object> newConstant(int id, String name) {
          return new AttributeKey<Object>(id, name);
        }
      };

  /**
   * Returns the singleton instance of the {@link AttributeKey} which has the specified {@code
   * name}.
   */
  @SuppressWarnings("unchecked")
  public static <T> AttributeKey<T> valueOf(String name) {
    return (AttributeKey<T>) pool.valueOf(name);
  }

  /** Returns {@code true} if a {@link AttributeKey} exists for the given {@code name}. */
  public static boolean exists(String name) {
    return pool.exists(name);
  }

  /**
   * Creates a new {@link AttributeKey} for the given {@code name} or fail with an {@link
   * IllegalArgumentException} if a {@link AttributeKey} for the given {@code name} exists.
   */
  @SuppressWarnings("unchecked")
  public static <T> AttributeKey<T> newInstance(String name) {
    return (AttributeKey<T>) pool.newInstance(name);
  }

  @SuppressWarnings("unchecked")
  public static <T> AttributeKey<T> valueOf(
      Class<?> firstNameComponent, String secondNameComponent) {
    return (AttributeKey<T>) pool.valueOf(firstNameComponent, secondNameComponent);
  }

  private AttributeKey(int id, String name) {
    super(id, name);
  }
}
