package xyz.vopen.framework.chaos.remoting.api;

/**
 * {@link Plugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public interface Plugin<T> extends NetMonitor<T> {

  boolean preProcess(Session session, T t);

  void stateEvent(StateMachineEnum stateMachineEnum, Session session, Throwable throwable);
}
