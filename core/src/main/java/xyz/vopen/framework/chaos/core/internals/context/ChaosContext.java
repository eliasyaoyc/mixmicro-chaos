package xyz.vopen.framework.chaos.core.internals.context;

import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.common.LifeCycle;
import xyz.vopen.framework.chaos.core.internals.EventManager;
import xyz.vopen.framework.chaos.core.internals.RecordAccumulator;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory;
import xyz.vopen.framework.chaos.core.internals.serializer.SerializerFactory;
import xyz.vopen.framework.chaos.core.processor.chain.Handler;
import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.NetMonitor;
import xyz.vopen.framework.chaos.remoting.api.Plugin;
import xyz.vopen.framework.chaos.remoting.api.Protocol;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import xyz.vopen.framework.chaos.core.internals.context.AbstractChaosContext.ConnectionSet;

/**
 * {@link ChaosContext}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/26
 */
public interface ChaosContext extends LifeCycle {

  /**
   * Returns the message process object. e.g. {@link
   * xyz.vopen.framework.chaos.core.processor.ServerMessageProcessor} and {@link
   * xyz.vopen.framework.chaos.core.processor.ClientMessageProcessor}.
   *
   * @return
   */
  MessageProcessor getProcessor();

  /**
   * Set the concrete processor method.
   *
   * @param processor
   */
  void setProcessor(MessageProcessor processor);

  /**
   * Returns the {@link SerializerFactory}.
   *
   * @return
   */
  SerializerFactory getSerializerFactory();

  /**
   * Returns the {@link ChaosRequestFactory}.
   *
   * @return
   */
  ChaosRequestFactory getRequestFactory();

  /**
   * Returns the processor handler chain.
   *
   * @return
   */
  Handler getHandler();

  /**
   * Returns the {@link RecordAccumulator} Used for store request.
   *
   * @return
   */
  RecordAccumulator getAccumulator();

  /**
   * Returns the {@link Protocol}.
   *
   * @return
   */
  Protocol getProtocol();

  /**
   * Returns the heartbeat plugin.
   *
   * @return
   */
  Plugin getHeartbeatPlugin(long interval, ChaosContext context);

  /**
   * Returns the {@link EventManager}.
   *
   * @return
   */
  EventManager getEventManager();

  /** Set the {@link EventManager}. */
  void setEventManager(EventManager eventManager);

  /**
   * Replace the services.
   *
   * @param services
   */
  void replace(String services);

  /**
   * Returns the {@link ConnectionSet}.
   *
   * @return
   */
  ConnectionSet getConnectionSet();

  /**
   * Returns the services.
   *
   * @return
   */
  ChaosConfig getConfig();

  /**
   * Returns Monitor.
   *
   * @return
   */
  NetMonitor getMonitor();

  void wakeupNotFull();

  Condition getNotFullCondition();

  ReentrantLock getNotFullLock();
}
