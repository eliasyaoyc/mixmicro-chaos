package xyz.vopen.framework.chaos.remoting.api;

/**
 * {@link Request}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/14
 */
public interface Request {

  /** Returns the request id. */
  String getRequestId();

  /**
   * Returns the request type.
   *
   * @return
   */
  int getType();

  /**
   * Returns the serializer type.
   *
   * @return
   */
  int getSerializerType();

  /**
   * Set the request clientId.
   *
   * @param clientId
   */
  void setClientId(String clientId);
}
