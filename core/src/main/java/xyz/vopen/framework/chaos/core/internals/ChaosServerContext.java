package xyz.vopen.framework.chaos.core.internals;

import org.springframework.util.StringUtils;
import xyz.vopen.framework.chaos.common.ChaosConfig;
import xyz.vopen.framework.chaos.core.internals.context.AbstractChaosContext;
import xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory;
import xyz.vopen.framework.chaos.core.processor.ServerMessageProcessor;
import xyz.vopen.framework.chaos.remoting.aio.ChaosServerConfig;
import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;

import java.util.List;

/**
 * {@link ChaosServerContext}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/26
 */
public class ChaosServerContext extends AbstractChaosContext {

  /** The configuration of {@link xyz.vopen.framework.chaos.core.ChaosCoordinator}. */
  private final ChaosServerConfig config;

  private MessageProcessor messageProcessor;
  private final ChaosRequestFactory chaosRequestFactory;
  private final EventManager eventManager;

  public ChaosServerContext(ChaosServerConfig config) {
    this.config = config;
    this.chaosRequestFactory = new ChaosRequestFactory(config);
    this.eventManager = new ChaosEventManager(config, chaosRequestFactory, getProtocol());
  }

  private void buildServerConfig() {
    this.config.setProcessor(this.messageProcessor);
    this.config.setProtocol(getProtocol());
  }

  @Override
  public void start() {
    super.start();
    this.messageProcessor = new ServerMessageProcessor(this);
    this.eventManager.initialize();
    buildServerConfig();
  }

  @Override
  public void destroy() {
    this.messageProcessor.destroy();
  }

  /**
   * Returns the {@link ServerMessageProcessor}.
   *
   * @return
   */
  @Override
  public MessageProcessor getProcessor() {
    return this.messageProcessor;
  }

  @Override
  public void setProcessor(MessageProcessor processor) {
    if (processor instanceof ServerMessageProcessor) {
      this.messageProcessor = processor;
    }
    throw new IllegalArgumentException(
        "Processor type is error, must be [ ServerMessageProcessor or subclass ]");
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
   * Returns the {@link xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager} Used for
   * store request, scope Server.
   *
   * @return
   */
  @Override
  public EventManager getEventManager() {
    return this.eventManager;
  }

  /**
   * Replace the services address.
   *
   * @param services
   */
  @Override
  public void replace(String services) {
    if (StringUtils.isEmpty(services)) {
      return;
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
}
