package xyz.vopen.framework.chaos.remoting.aio.processor;

import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.NetMonitor;
import xyz.vopen.framework.chaos.remoting.api.Plugin;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

import java.net.URL;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link AbstractMessageProcessor}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public abstract class AbstractMessageProcessor<T> implements MessageProcessor<T>, NetMonitor<T> {

  private List<Plugin<T>> plugins = new ArrayList<>();

  @Override
  public void process(Session session, T msg) {
    boolean flag = true;
    for (Plugin<T> plugin : plugins) {
      if (!plugin.preProcess(session, msg)) {
        flag = false;
      }
    }
    if (flag) {
      process0(session, msg);
    }
  }

  @Override
  public void stateEvent(
      Session session, StateMachineEnum stateMachineEnum, Throwable throwable) {
    for (Plugin<T> plugin : plugins) {
      plugin.stateEvent(stateMachineEnum, session, throwable);
    }
    stateEvent0(session, stateMachineEnum, throwable);
  }

  @Override
  public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
    AsynchronousSocketChannel acceptChannel = channel;
    for (Plugin<T> plugin : plugins) {
      acceptChannel = plugin.shouldAccept(acceptChannel);
      if (acceptChannel == null) {
        return null;
      }
    }
    return acceptChannel;
  }

  @Override
  public void afterRead(Session session, int readSize) {
    for (Plugin<T> plugin : plugins) {
      plugin.afterRead(session, readSize);
    }
  }

  @Override
  public void beforeRead(Session session) {
    for (Plugin<T> plugin : plugins) {
      plugin.beforeRead(session);
    }
  }

  @Override
  public void afterWrite(Session session, int writeSize) {
    for (Plugin<T> plugin : plugins) {
      plugin.afterWrite(session, writeSize);
    }
  }

  @Override
  public void beforeWrite(Session session) {
    for (Plugin<T> plugin : plugins) {
      plugin.beforeWrite(session);
    }
  }

  @Override
  public final void addPlugin(Plugin plugin) {
    this.plugins.add(plugin);
  }

  public abstract void process0(Session session, T msg);

  public abstract void stateEvent0(
      Session session, StateMachineEnum stateMachineEnum, Throwable throwable);


  @Override
  public boolean isRunning() {
    return false;
  }

  @Override
  public void start() {

  }
}
