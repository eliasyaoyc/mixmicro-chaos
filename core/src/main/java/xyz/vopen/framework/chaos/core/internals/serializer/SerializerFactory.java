package xyz.vopen.framework.chaos.core.internals.serializer;

import xyz.vopen.framework.chaos.core.internals.serializer.impl.ChaosHessian;
import xyz.vopen.framework.chaos.core.internals.Serializer;
import xyz.vopen.framework.chaos.core.internals.serializer.impl.ChaosJSON;

/**
 * {@link SerializerFactory}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/17
 */
public class SerializerFactory {

  public static SerializerFactory getInstance() {
    return SerializerFactoryLazyHolder.INSTANCE;
  }

  static class SerializerFactoryLazyHolder {
    static final SerializerFactory INSTANCE = new SerializerFactory();
  }

  public Serializer getSerializer(int type) {
    Serializer serializer;
    switch (type) {
      case 1:
        serializer = ChaosJSON.getInstance();
        break;
      case 2:
        serializer = ChaosHessian.getInstance();
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
    return serializer;
  }
}
