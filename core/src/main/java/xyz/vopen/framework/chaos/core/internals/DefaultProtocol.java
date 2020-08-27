package xyz.vopen.framework.chaos.core.internals;

import com.alibaba.fastjson.JSON;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.request.ChaosResponse;
import xyz.vopen.framework.chaos.core.internals.serializer.SerializerFactory;
import xyz.vopen.framework.chaos.remoting.aio.transport.WriterBuffer;
import xyz.vopen.framework.chaos.remoting.api.Protocol;
import xyz.vopen.framework.chaos.remoting.api.Request;
import xyz.vopen.framework.chaos.remoting.api.Session;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * {@link DefaultProtocol}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/17
 */
public class DefaultProtocol implements Protocol<Request> {

  private static final int INTEGER_BYTES = Integer.SIZE / Byte.SIZE;

  private final SerializerFactory factory;

  public DefaultProtocol(SerializerFactory factory) {
    this.factory = factory;
  }

  @Override
  public byte[] encode(Request request, Session session) {
    Serializer serializer = this.factory.getSerializer(request.getSerializerType());
    byte[] data = (byte[]) serializer.serializer(request);
    return data;
  }

  @Override
  public synchronized Request decode(ByteBuffer readBuffer, Session session) {
    try {
      int remaining = readBuffer.remaining();
      if (remaining < INTEGER_BYTES) {
        return null;
      }
      // protocol type.
      int protocolType = readBuffer.getInt();
      // data length.
      int messageSize = readBuffer.getInt();
      if (messageSize > remaining) {
        return null;
      }
      int requestType = readBuffer.getInt();

      if (messageSize > remaining || messageSize < INTEGER_BYTES){
        return null;
      }
      byte[] data = new byte[messageSize - INTEGER_BYTES];
      Serializer serializer = this.factory.getSerializer(protocolType);
      readBuffer.get(data);
      Request req;
      if (requestType == 1) {
        req = JSON.parseObject(data, ChaosRequest.class);
      } else {
        req = JSON.parseObject(data, ChaosResponse.class);
      }
      return req;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public boolean send0(Request request, Session session) {
    byte[] data =
        (byte[]) this.factory.getSerializer(request.getSerializerType()).serializer(request);

    WriterBuffer writerBuffer = (WriterBuffer) session.writeBuffer();
    try {
      writerBuffer.writeInt(request.getSerializerType());
      writerBuffer.writeInt(data.length + 4);
      if (request instanceof ChaosRequest) {
        writerBuffer.writeInt((byte) 1);
      } else {
        writerBuffer.writeInt((byte) 2);
      }
      writerBuffer.write(data);
      writerBuffer.flush();
      return true;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }
}
