package xyz.vopen.framework.chaos.core.internals.serializer.impl;

import com.alibaba.fastjson.JSON;
import xyz.vopen.framework.chaos.core.internals.serializer.AbstractSerializer;
import xyz.vopen.framework.chaos.remoting.api.Request;

import java.nio.ByteBuffer;

/**
 * {@link ChaosJSON} Implementation {@link xyz.vopen.framework.chaos.core.internals.Serializer}.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/20
 */
public class ChaosJSON extends AbstractSerializer {

  /** Singleton. */
  public static ChaosJSON getInstance(){
    return ChaosJSONLazyHolder.INSTANCE;
  }

  static class ChaosJSONLazyHolder{
    static final ChaosJSON INSTANCE = new ChaosJSON();
  }

  @Override
  public Object serializer(Request request) {
    return JSON.toJSONBytes(request);
  }

  @Override
  public Object deserializer(ByteBuffer data) {
    byte[] b = new byte[data.position()];
    data.get(b, 0, b.length);
    return JSON.parseObject(b, Request.class);
  }
}
