package xyz.vopen.framework.chaos.remoting.api;

/**
 * {@link StateMachineEnum}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public enum StateMachineEnum {

  /**
   * creation connection and session object.
   */
  NEW_SESSION,

  /** channel already closed. */
  INPUT_SHUTDOWN,

  /** biz process error. */
  PROCESS_EXCEPTION,

  /** decode error. */
  DECODE_EXCEPTION,

  /**
   * the operation of read encounter error {@link
   * java.nio.channels.CompletionHandler#failed(Throwable, Object)}.
   */
  INPUT_EXCEPTION,

  /**
   * the operation of write encounter error {@link
   * java.nio.channels.CompletionHandler#failed(Throwable, Object)}.
   */
  OUTPUT_EXCEPTION,

  /** session is closing. */
  SESSION_CLOSING,

  /** session closed success. */
  SESSION_CLOSED,

  /** Reject accept connection(e.g. too many connection, service error etc.) ,only scope service. */
  REJECT_ACCEPT,

  /** Service-wide accept error. */
  ACCEPT_EXCEPTION;
}
