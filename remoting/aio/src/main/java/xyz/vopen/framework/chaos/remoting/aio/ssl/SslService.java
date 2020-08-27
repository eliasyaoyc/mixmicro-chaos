package xyz.vopen.framework.chaos.remoting.aio.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.remoting.aio.buffer.BufferPage;
import xyz.vopen.framework.chaos.remoting.aio.exception.SslException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * {@link SslService} TLS /SSL service.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class SslService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SslService.class);

  private SSLContext sslContext;

  private boolean isClient;

  private ClientAuth clientAuth;

  private CompletionHandler<Integer, HandshakeModel> handshakeModelCompletionHandler =
      new CompletionHandler<Integer, HandshakeModel>() {
        @Override
        public void completed(Integer result, HandshakeModel attachment) {
          if (result == -1) {
            attachment.setEof(true);
          }
          synchronized (attachment) {
            doHandshake(attachment);
          }
        }

        @Override
        public void failed(Throwable exc, HandshakeModel attachment) {
          attachment.setEof(true);
          attachment.getHandshakeCallback().callback();
        }
      };

  public SslService(boolean isClient, ClientAuth clientAuth) {
    this.isClient = isClient;
    this.clientAuth = clientAuth;
  }

  public void initKeyStore(
      InputStream keyStoreInputStream, String keyStorePassword, String keyPassword) {
    try {
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(keyStoreInputStream, keyStorePassword.toCharArray());
      kmf.init(ks, keyPassword.toCharArray());
      KeyManager[] keyManagers = kmf.getKeyManagers();

      sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagers, null, new SecureRandom());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void initTrust(InputStream inputStream, String trustPassword) {
    try {
      TrustManager[] trustManagers;
      if (inputStream != null) {
        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(inputStream, trustPassword.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);
        trustManagers = tmf.getTrustManagers();
      } else {
        trustManagers =
            new TrustManager[] {
              new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                    throws CertificateException {}

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                    throws CertificateException {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                  return new X509Certificate[0];
                }
              }
            };
      }
      sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustManagers, new SecureRandom());
    } catch (Exception e) {
      throw new SslException(e);
    }
  }

  public HandshakeModel createSSLEngine(
      AsynchronousSocketChannel socketChannel, BufferPage bufferPage) {
    try {
      SSLEngine sslEngine = sslContext.createSSLEngine();
      SSLSession session = sslEngine.getSession();
      sslEngine.setUseClientMode(isClient);
      if (clientAuth != null) {
        switch (clientAuth) {
          case OPTIONAL:
            sslEngine.setWantClientAuth(true);
            break;
          case REQUIRE:
            sslEngine.setNeedClientAuth(true);
            break;
          case NONE:
            break;
          default:
            throw new SslException("unknown auth" + clientAuth);
        }
      }
      HandshakeModel handshakeModel =
          HandshakeModel.builder()
              .sslEngine(sslEngine)
              .appWriteBuffer(bufferPage.allocate(session.getApplicationBufferSize()))
              .netWriteBuffer(bufferPage.allocate(session.getPacketBufferSize()))
              .appReadBuffer(bufferPage.allocate(session.getApplicationBufferSize()))
              .netReadBuffer(bufferPage.allocate(session.getPacketBufferSize()))
              .socketChannel(socketChannel)
              .build();
      handshakeModel.getNetWriteBuffer().buffer().flip();
      sslEngine.beginHandshake();
      return handshakeModel;
    } catch (Exception e) {
      throw new SslException(e);
    }
  }

  /**
   * asynchronous handshake.
   *
   * @param handshakeModel
   */
  public void doHandshake(HandshakeModel handshakeModel) {
    SSLEngineResult result = null;
    try {
      SSLEngineResult.HandshakeStatus handshakeStatus = null;
      ByteBuffer netReadBuffer = handshakeModel.getNetReadBuffer().buffer();
      ByteBuffer appReadBuffer = handshakeModel.getAppReadBuffer().buffer();
      ByteBuffer netWriteBuffer = handshakeModel.getNetWriteBuffer().buffer();
      ByteBuffer appWriteBuffer = handshakeModel.getAppWriteBuffer().buffer();
      SSLEngine engine = handshakeModel.getSslEngine();

      // network disconnect during handshake phase.
      if (handshakeModel.isEof()) {
        LOGGER.info("the ssl handshake is terminated.");
        handshakeModel.getHandshakeCallback().callback();
        return;
      }
      while (!handshakeModel.isFinished()) {
        handshakeStatus = engine.getHandshakeStatus();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("handshake state : {}", handshakeStatus);
        }
        switch (handshakeStatus) {
          case NEED_UNWRAP:
            // decode.
            netReadBuffer.flip();
            if (netReadBuffer.hasRemaining()) {
              result = engine.unwrap(netReadBuffer, appReadBuffer);
              netReadBuffer.compact();
            } else {
              netReadBuffer.clear();
              handshakeModel
                  .getSocketChannel()
                  .read(netReadBuffer, handshakeModel, handshakeModelCompletionHandler);
              return;
            }
            if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
              handshakeModel.setFinished(true);
              netReadBuffer.clear();
            }
            switch (result.getStatus()) {
              case OK:
                break;
              case BUFFER_OVERFLOW:
                LOGGER.warn("do Handshake buffer overflow.");
                break;
              case BUFFER_UNDERFLOW:
                LOGGER.warn("do Handshake buffer unOverflow.");
                return;
              default:
                throw new IllegalArgumentException("Invalid SSL status: " + result.getStatus());
            }
            break;
          case NEED_WRAP:
            if (netWriteBuffer.hasRemaining()) {
              LOGGER.info("the data has not been output.");
              handshakeModel
                  .getSocketChannel()
                  .write(netWriteBuffer, handshakeModel, handshakeModelCompletionHandler);
              return;
            }
            netWriteBuffer.clear();
            result = engine.wrap(appWriteBuffer, netWriteBuffer);
            switch (result.getStatus()) {
              case OK:
                appWriteBuffer.clear();
                netWriteBuffer.flip();
                if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                  handshakeModel.setFinished(true);
                }
                handshakeModel
                    .getSocketChannel()
                    .write(netWriteBuffer, handshakeModel, handshakeModelCompletionHandler);
                return;
              case BUFFER_OVERFLOW:
                LOGGER.warn("need wrap buffer overflow.");
                break;
              case BUFFER_UNDERFLOW:
                throw new SslException(
                    "buffer underflow occur after a wrap. dont think we should ever get here.");
              case CLOSED:
                LOGGER.warn("closed");
                try {
                  netWriteBuffer.flip();
                  netReadBuffer.flip();
                } catch (Exception e) {
                  LOGGER.error(
                      "failed to send server's close message due to socket channel's failure.");
                }
                break;
              default:
                throw new IllegalArgumentException("Invalid SSL status : " + result.getStatus());
            }
            break;
          case NEED_TASK:
            Runnable task;
            while ((task = engine.getDelegatedTask()) != null) {
              task.run();
            }
            break;
          case FINISHED:
            LOGGER.warn("handshake finished.");
            break;
          case NOT_HANDSHAKING:
            LOGGER.error("not handshaking.");
            break;
          default:
            throw new IllegalStateException("Invalid SSl status : " + handshakeStatus);
        }
      }
      LOGGER.info("handshake completed.");
      handshakeModel.getHandshakeCallback().callback();
    } catch (Exception e) {
      LOGGER.error("ignore doHandshake exception :{}", e.getMessage());
      handshakeModel.setEof(true);
      handshakeModel.getHandshakeCallback().callback();
    }
  }
}
