package xyz.vopen.framework.chaos.core.test.rpc.rpc;

import java.io.Serializable;

public class RpcResponse implements Serializable {
  /** 消息的唯一标示，与对应的RpcRequest uuid值相同 */
  private String uuid;
  /** 返回对象 */
  private Object returnObject;

  /** 返回对象类型 */
  private String returnType;

  /** 异常 */
  private String exception;

  public RpcResponse(String uuid) {
    this.uuid = uuid;
  }

  public Object getReturnObject() {
    return returnObject;
  }

  public void setReturnObject(Object returnObject) {
    this.returnObject = returnObject;
  }

  public String getReturnType() {
    return returnType;
  }

  public void setReturnType(String returnType) {
    this.returnType = returnType;
  }

  public String getException() {
    return exception;
  }

  public void setException(String exception) {
    this.exception = exception;
  }

  public String getUuid() {
    return uuid;
  }
}
