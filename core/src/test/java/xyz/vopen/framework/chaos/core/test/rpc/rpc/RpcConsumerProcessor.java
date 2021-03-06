//package xyz.vopen.framework.chaos.core.test.rpc.rpc;
//
//import org.apache.commons.lang3.ArrayUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import xyz.vopen.framework.chaos.remoting.aio.transport.WriterBuffer;
//import xyz.vopen.framework.chaos.remoting.api.Buffer;
//import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
//import xyz.vopen.framework.chaos.remoting.api.Plugin;
//import xyz.vopen.framework.chaos.remoting.api.Session;
//import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.ObjectInput;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutput;
//import java.io.ObjectOutputStream;
//import java.lang.reflect.Proxy;
//import java.net.SocketTimeoutException;
//import java.net.URL;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//
//public class RpcConsumerProcessor implements MessageProcessor<byte[]> {
//  private static final Logger LOGGER = LoggerFactory.getLogger(RpcConsumerProcessor.class);
//  private Map<String, CompletableFuture<RpcResponse>> synchRespMap = new ConcurrentHashMap<>();
//  private Map<Class, Object> objectMap = new ConcurrentHashMap<>();
//  private Session<byte[]> aioSession;
//
//  public static void main(String[] args) {
//    CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
//    new Thread(
//            () -> {
//              try {
//                System.out.println(completableFuture.get());
//              } catch (InterruptedException e) {
//                e.printStackTrace();
//              } catch (ExecutionException e) {
//                e.printStackTrace();
//              }
//            })
//        .start();
//
//    new Thread(
//            () -> {
//              try {
//                Thread.sleep(2000);
//              } catch (InterruptedException e) {
//                e.printStackTrace();
//              }
//              completableFuture.complete(null);
//            })
//        .start();
//  }
//
//  @Override
//  public void process(Session<byte[]> session, byte[] msg) {
//    ObjectInput objectInput = null;
//    try {
//      objectInput = new ObjectInputStream(new ByteArrayInputStream(msg));
//      RpcResponse resp = (RpcResponse) objectInput.readObject();
//      synchRespMap.get(resp.getUuid()).complete(resp);
//    } catch (Exception e) {
//      e.printStackTrace();
//    } finally {
//      if (objectInput != null) {
//        try {
//          objectInput.close();
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
//    }
//  }
//
//  public <T> T getObject(final Class<T> remoteInterface) {
//    Object obj = objectMap.get(remoteInterface);
//    if (obj != null) {
//      return (T) obj;
//    }
//    obj =
//        (T)
//            Proxy.newProxyInstance(
//                getClass().getClassLoader(),
//                new Class[] {remoteInterface},
//                (proxy, method, args) -> {
//                  RpcRequest req = new RpcRequest();
//                  req.setInterfaceClass(remoteInterface.getName());
//                  req.setMethod(method.getName());
//                  Class<?>[] types = method.getParameterTypes();
//                  if (!ArrayUtils.isEmpty(types)) {
//                    String[] paramClass = new String[types.length];
//                    for (int i = 0; i < types.length; i++) {
//                      paramClass[i] = types[i].getName();
//                    }
//                    req.setParamClassList(paramClass);
//                  }
//                  req.setParams(args);
//
//                  RpcResponse rmiResp = sendRpcRequest(req);
//                  if (StringUtils.isNotBlank(rmiResp.getException())) {
//                    throw new RuntimeException(rmiResp.getException());
//                  }
//                  return rmiResp.getReturnObject();
//                });
//    objectMap.put(remoteInterface, obj);
//    return (T) obj;
//  }
//
//  private final RpcResponse sendRpcRequest(RpcRequest request) throws Exception {
//    CompletableFuture<RpcResponse> rpcResponseCompletableFuture = new CompletableFuture<>();
//    synchRespMap.put(request.getUuid(), rpcResponseCompletableFuture);
//
//    // 输出消息
//    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//    ObjectOutput objectOutput = new ObjectOutputStream(byteArrayOutputStream);
//    objectOutput.writeObject(request);
//    byte[] data = byteArrayOutputStream.toByteArray();
//    synchronized (aioSession) {
//      WriterBuffer writerBuffer = (WriterBuffer) aioSession.writeBuffer();
//      writerBuffer.writeInt(data.length + 4);
//      writerBuffer.write(data);
//      writerBuffer.flush();
//    }
//    //        aioSession.write(byteArrayOutputStream.toByteArray());
//
//    try {
//      RpcResponse resp = rpcResponseCompletableFuture.get(3, TimeUnit.SECONDS);
//      return resp;
//    } catch (Exception e) {
//      throw new SocketTimeoutException("Message is timeout!");
//    }
//  }
//
//  @Override
//  public void stateEvent(
//      Session<byte[]> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
//    switch (stateMachineEnum) {
//      case NEW_SESSION:
//        this.aioSession = session;
//        break;
//    }
//  }
//
//  @Override
//  public void addPlugin(Plugin plugin) {}
//
//  @Override
//  public URL getUrl() {
//    return null;
//  }
//
//  @Override
//  public boolean isRunning() {
//    return false;
//  }
//
//  @Override
//  public void start() {}
//
//  @Override
//  public void destroy() {}
//}
