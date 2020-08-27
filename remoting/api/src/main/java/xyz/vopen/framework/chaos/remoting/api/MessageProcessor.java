package xyz.vopen.framework.chaos.remoting.api;

import xyz.vopen.framework.chaos.common.LifeCycle;

/**
 * {@link MessageProcessor}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public interface MessageProcessor<T> extends LifeCycle {

   void process(Session session, T msg);

   void stateEvent(Session session, StateMachineEnum stateMachineEnum, Throwable throwable);

   void addPlugin(Plugin plugin);

}
