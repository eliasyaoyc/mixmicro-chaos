package xyz.vopen.framework.chaos.core.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferFactory;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPagePool;
import xyz.vopen.framework.chaos.remoting.aio.exception.SslException;
import xyz.vopen.framework.chaos.remoting.aio.ssl.ClientAuth;
import xyz.vopen.framework.chaos.remoting.aio.ssl.SslAsynchronousSocketChannel;
import xyz.vopen.framework.chaos.remoting.aio.ssl.SslService;
import xyz.vopen.framework.chaos.remoting.api.AbstractPlugin;

import java.io.InputStream;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * {@link SslPlugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class SslPlugin<T> extends AbstractPlugin<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SslPlugin.class);

  private SslService sslService;
  private BufferPagePool bufferPagePool;
  private boolean init = false;

  public SslPlugin() {
    this.bufferPagePool = BufferFactory.DISABLED_BUFFER_FACTORY.create();
  }

  public SslPlugin(BufferPagePool bufferPagePool) {
    this.bufferPagePool = bufferPagePool;
  }

  public void initForServer(
      InputStream keyStoreInputStream,
      String keyStorePassword,
      String keyPassword,
      ClientAuth clientAuth) {
    initCheck();
    sslService = new SslService(false, clientAuth);
    sslService.initKeyStore(keyStoreInputStream, keyStorePassword, keyPassword);
  }

  public void initForClient() {
    initForClient(null, null);
  }

  public void initForClient(InputStream inputStream, String trustPassword) {
    initCheck();
    sslService = new SslService(true, null);
    sslService.initTrust(inputStream, trustPassword);
  }

  private void initCheck() {
    if (init) {
      throw new SslException("plugin is already init.");
    }
    init = true;
  }

  @Override
  public AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
    return new SslAsynchronousSocketChannel(
        channel, sslService, bufferPagePool.allocateBufferPage());
  }
}
