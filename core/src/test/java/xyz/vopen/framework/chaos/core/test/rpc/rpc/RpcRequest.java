package xyz.vopen.framework.chaos.core.test.rpc.rpc;

import java.io.Serializable;
import java.util.UUID;

public class RpcRequest implements Serializable {

  /** 消息的唯一标识 */
  private final String uuid = UUID.randomUUID().toString();

  /** 接口名称 */
  private String interfaceClass;

  /** 调用方法 */
  private String method;

  /** 参数类型字符串 */
  private String[] paramClassList;

  /** 入参 */
  private Object[] params;

  public String getInterfaceClass() {
    return interfaceClass;
  }

  public void setInterfaceClass(String interfaceClass) {
    this.interfaceClass = interfaceClass;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public Object[] getParams() {
    return params;
  }

  public void setParams(Object... params) {
    this.params = params;
  }

  public String[] getParamClassList() {
    return paramClassList;
  }

  public void setParamClassList(String... paramClassList) {
    this.paramClassList = paramClassList;
  }

  public String getUuid() {
    return uuid;
  }
}
