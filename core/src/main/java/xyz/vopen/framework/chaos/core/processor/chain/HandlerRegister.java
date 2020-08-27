package xyz.vopen.framework.chaos.core.processor.chain;

import xyz.vopen.framework.chaos.core.processor.chain.impl.ConfirmHandler;
import xyz.vopen.framework.chaos.core.processor.chain.impl.ExitResHandler;
import xyz.vopen.framework.chaos.core.processor.chain.impl.HeartbeatHandler;
import xyz.vopen.framework.chaos.core.processor.chain.impl.OfflineHandler;
import xyz.vopen.framework.chaos.core.processor.chain.impl.OnlineHandler;
import xyz.vopen.framework.chaos.core.processor.chain.impl.InitializationResHandler;
import xyz.vopen.framework.chaos.core.processor.chain.impl.ShakeHandHandler;
import xyz.vopen.framework.chaos.core.processor.chain.impl.StartedResHandler;
import xyz.vopen.framework.chaos.core.processor.chain.impl.SyncHandler;

/**
 * {@link HandlerRegister}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/14
 */
public enum HandlerRegister {

  INSTANCE;

  private Handler handler;

  // TODO: 2020/7/18  spi optimize.
  HandlerRegister() {
    this.handler = new HeartbeatHandler();
    OfflineHandler offlineHandler = new OfflineHandler();
    OnlineHandler onlineHandler = new OnlineHandler();
    SyncHandler syncHandler = new SyncHandler();
    ConfirmHandler confirmHandler = new ConfirmHandler();
    ExitResHandler exitResHandler = new ExitResHandler();
    InitializationResHandler readyResHandler = new InitializationResHandler();
    StartedResHandler startResHandler = new StartedResHandler();
    ShakeHandHandler shakeHandHandler = new ShakeHandHandler();
    
    handler.setSuccessor(shakeHandHandler);
    shakeHandHandler.setSuccessor(offlineHandler);
    offlineHandler.setSuccessor(onlineHandler);
    onlineHandler.setSuccessor(syncHandler);
    syncHandler.setSuccessor(confirmHandler);
    confirmHandler.setSuccessor(exitResHandler);
    exitResHandler.setSuccessor(readyResHandler);
    readyResHandler.setSuccessor(startResHandler);
  }
  
  public Handler getHandler() {
    return this.handler;
  }
}
