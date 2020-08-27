//package xyz.vopen.framework.chaos.core.test.rpc.rpc;
//
//import xyz.vopen.framework.chaos.remoting.api.Protocol;
//import xyz.vopen.framework.chaos.remoting.api.Session;
//
//import java.nio.ByteBuffer;
//
//public class RpcProtocol implements Protocol<byte[]> {
//  private static final int INTEGER_BYTES = Integer.SIZE / Byte.SIZE;
//
//  @Override
//  public byte[] encode(ByteBuffer writerBuffer, Session<byte[]> session) {
//    return new byte[0];
//  }
//
//  @Override
//  public byte[] decode(ByteBuffer readBuffer, Session<byte[]> session) {
//    int remaining = readBuffer.remaining();
//    if (remaining < INTEGER_BYTES) {
//      return null;
//    }
//    int messageSize = readBuffer.getInt(readBuffer.position());
//    if (messageSize > remaining) {
//      return null;
//    }
//    byte[] data = new byte[messageSize - INTEGER_BYTES];
//    readBuffer.getInt();
//    readBuffer.get(data);
//    return data;
//  }
//}
