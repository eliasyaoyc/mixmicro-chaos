package xyz.vopen.framework.chaos.core.internals.serializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.core.internals.Serializer;

import java.nio.ByteBuffer;

/**
 * {@link AbstractSerializer}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/13
 */
public abstract class AbstractSerializer implements Serializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSerializer.class);

  /** Currently version, only support the hessian serialization. */
  private static final SerializerType DEFAULT_PROTOCOL_TYPE = SerializerType.HESSIAN;


  @Override
  public Object serializer(Request request) {
    return null;
  }

  @Override
  public Object deserializer(ByteBuffer byteBuffer) {
    return null;
  }

  @Override
  public SerializerType getSerializationType() {
    return DEFAULT_PROTOCOL_TYPE;
  }

  protected boolean checkIsValidator(Object object) {
    if (object == null) {
      LOGGER.info(
          "The object that [ SerializerFactory ] is empty during Hessian serialization... please check it.");
      return false;
    }
    return true;
  }
}
