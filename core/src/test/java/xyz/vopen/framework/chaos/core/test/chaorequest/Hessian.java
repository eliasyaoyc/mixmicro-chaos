package xyz.vopen.framework.chaos.core.test.chaorequest;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.core.exception.ChaosSerializerException;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.serializer.AbstractSerializer;
import xyz.vopen.framework.chaos.remoting.api.Request;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * {@link Hessian}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/13
 */
public class Hessian extends AbstractSerializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(Hessian.class);

  /**
   * Initial SerializerFactory, it is highly recommended to cache this factory for every
   * serialization and deserialization.
   */
  private SerializerFactory serializerFactory;

  public Hessian(SerializerFactory serializerFactory) {
    this.serializerFactory = serializerFactory;
  }

  // singleton.
  public static Hessian getInstance() {
    return HessianLazyHolder.INSTANCE;
  }

  static class HessianLazyHolder {
    public static final Hessian INSTANCE = new Hessian(new SerializerFactory());
  }

  @Override
  public Object serializer(Request request) {
    if (!checkIsValidator(serializerFactory)) {
      return null;
    }
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Hessian2Output hout = new Hessian2Output(out);
      hout.setSerializerFactory(serializerFactory);
      hout.writeObject(request);
      hout.close();
      return out.toByteArray();
    } catch (IOException e) {
      throw new ChaosSerializerException("chaos hessian serializer error : {}", e);
    }
  }

  @Override
  public Object deserializer(ByteBuffer data) {
    if (!checkIsValidator(serializerFactory)) {
      return null;
    }
    try {
      ByteArrayInputStream bin = new ByteArrayInputStream(data.array(), 0, data.limit());
      Hessian2Input hin = new Hessian2Input(bin);
      hin.setSerializerFactory(new SerializerFactory());
      ChaosRequest chaosRequest = (ChaosRequest) hin.readObject();
      hin.close();
      return chaosRequest;
    } catch (IOException e) {
      throw new ChaosSerializerException("chaos hessian deserializer error : {}", e);
    }
  }

  public Object deserializer(byte[] data) {
    if (!checkIsValidator(serializerFactory)) {
      return null;
    }
    try {
      ByteArrayInputStream bin = new ByteArrayInputStream(data, 0, data.length);
      Hessian2Input hin = new Hessian2Input(bin);
      hin.setSerializerFactory(new SerializerFactory());
      Request chaosRequest = (Request) hin.readObject();
      hin.close();
      return chaosRequest;
    } catch (IOException e) {
      throw new ChaosSerializerException("chaos hessian deserializer error : {}", e);
    }
  }
}
