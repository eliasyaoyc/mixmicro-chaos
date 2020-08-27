package xyz.vopen.framework.chaos.remoting.api;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * {@link AbstractPlugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class AbstractPlugin<T> implements Plugin<T>{

    @Override
    public boolean preProcess(Session session,T t) {
        return true;
    }

    @Override
    public void stateEvent(StateMachineEnum stateMachineEnum, Session session, Throwable throwable) {

    }

    @Override
    public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return channel;
    }

    @Override
    public void afterRead(Session session, int readSize) {

    }

    @Override
    public void beforeRead(Session session) {

    }

    @Override
    public void afterWrite(Session session, int writeSize) {

    }

    @Override
    public void beforeWrite(Session session) {

    }
}
