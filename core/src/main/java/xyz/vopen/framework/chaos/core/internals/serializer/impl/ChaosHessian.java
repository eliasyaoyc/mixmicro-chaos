package xyz.vopen.framework.chaos.core.internals.serializer.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import xyz.vopen.framework.chaos.core.exception.ChaosSerializerException;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.serializer.AbstractSerializer;
import xyz.vopen.framework.chaos.remoting.api.Request;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * {@link ChaosHessian} Implementation {@link xyz.vopen.framework.chaos.core.internals.Serializer}.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/13
 */
public class ChaosHessian extends AbstractSerializer {

  /** Singleton. */
  public static ChaosHessian getInstance(){
    return ChaosHessianLazyHolder.INSTANCE;
  }

  static class ChaosHessianLazyHolder{
    static final ChaosHessian INSTANCE = new ChaosHessian();
  }

  /**
   * Initial SerializerFactory, it is highly recommended to cache this factory for every
   * serialization and deserialization.
   */
  private SerializerFactory serializerFactory = new SerializerFactory();

  public ChaosHessian() {
  }

  @Override
  public Object serializer(Request request) {
    if (!checkIsValidator(serializerFactory)) {
      return null;
    }
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Hessian2Output hout = new Hessian2Output(out);
      hout.setSerializerFactory(new SerializerFactory());
      hout.writeObject(request);
      hout.close();
      return out.toByteArray();
    } catch (Exception e) {
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
    } catch (Exception e) {
      throw new ChaosSerializerException("chaos hessian deserializer error : {}", e);
    }
  }
}
