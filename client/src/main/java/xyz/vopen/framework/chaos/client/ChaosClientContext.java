package xyz.vopen.framework.chaos.client;

import org.springframework.util.StringUtils;
import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.core.internals.DefaultProtocol;
import xyz.vopen.framework.chaos.core.internals.EventManager;
import xyz.vopen.framework.chaos.core.internals.RecordAccumulator;
import xyz.vopen.framework.chaos.core.internals.context.AbstractChaosContext;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory;
import xyz.vopen.framework.chaos.core.internals.serializer.SerializerFactory;
import xyz.vopen.framework.chaos.core.plugins.InitializationPlugin;
import xyz.vopen.framework.chaos.core.plugins.ReconnectPlugin;
import xyz.vopen.framework.chaos.core.plugins.ShakeHandPlugin;
import xyz.vopen.framework.chaos.core.processor.ClientMessageProcessor;
import xyz.vopen.framework.chaos.core.processor.chain.Handler;
import xyz.vopen.framework.chaos.core.processor.chain.impl.HeartbeatHandler;
import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.Plugin;
import xyz.vopen.framework.chaos.remoting.api.Protocol;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link ChaosClientContext} The context scope for {@link ChaosClient}.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/26
 */
public class ChaosClientContext extends AbstractChaosContext {

  /** The configuration of {@link ChaosClient}. */
  private final ChaosClientConfig config;

  private final ChaosRequestFactory chaosRequestFactory;
  private final RecordAccumulator accumulator;
  private MessageProcessor clientProcessor;
  private EventManager eventManager;

  public ChaosClientContext(ChaosClientConfig config) {
    this.config = config;
    this.chaosRequestFactory = new ChaosRequestFactory(this.config);
    this.accumulator = new RecordAccumulator(config);

    start();
  }

  @Override
  public void start() {
    super.start();
    // build processor for client.
    buildClientProcessor();
    this.config.setProtocol(new DefaultProtocol(getSerializerFactory()));
    this.config.setProcessor(this.clientProcessor);

    String destinations = this.config.getServices();

    super.setConnectionSet(
        new ConnectionSet(super.convertConnectPair(destinations, config.getPort())));
  }

  private void buildClientProcessor() {
    clientProcessor = new ClientMessageProcessor(this);
    clientProcessor.addPlugin(new ShakeHandPlugin(this));
    clientProcessor.addPlugin(getHeartbeatPlugin(config.getHeartbeatInterval(), this));
    clientProcessor.addPlugin(new InitializationPlugin(this));
    clientProcessor.addPlugin(new ReconnectPlugin(this));
  }

  @Override
  public MessageProcessor getProcessor() {
    return clientProcessor;
  }

  @Override
  public void setProcessor(MessageProcessor processor) {
    if (processor instanceof ClientMessageProcessor) {
      this.clientProcessor = processor;
    }
    throw new IllegalArgumentException(
        "Processor type is error, must be [ ClientMessageProcessor or subclass]");
  }

  /**
   * Returns the {@link ChaosRequestFactory}.
   *
   * @return
   */
  @Override
  public ChaosRequestFactory getRequestFactory() {
    return this.chaosRequestFactory;
  }

  /**
   * Returns the {@link RecordAccumulator} Used for store request, scope Client.
   *
   * @return
   */
  @Override
  public RecordAccumulator getAccumulator() {
    return this.accumulator;
  }

  @Override
  public void replace(String services) {
    if (StringUtils.isEmpty(services)) {
     }
    List<ConnectionPair> connectionPairs =
            super.convertConnectPair(services, this.config.getPort());
    super.setConnectionSet(new ConnectionSet(connectionPairs));
  }

  /**
   * Returns the services.
   *
   * @return
   */
  @Override
  public ChaosConfig getConfig() {
    return this.config;
  }

  @Override
  public EventManager getEventManager() {
    return this.eventManager;
  }

  @Override
  public void setEventManager(EventManager eventManager) {
    this.eventManager = eventManager;
  }
}
