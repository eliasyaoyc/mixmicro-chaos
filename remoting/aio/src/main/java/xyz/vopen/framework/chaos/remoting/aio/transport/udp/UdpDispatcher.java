package xyz.vopen.framework.chaos.remoting.aio.transport.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.remoting.aio.transport.WriterBuffer;
import xyz.vopen.framework.chaos.remoting.api.Buffer;
import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * {@link UdpDispatcher}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class UdpDispatcher<T> implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(UdpDispatcher.class);

  public final RequestTask<T> EXECUTE_TASK_OR_SHUTDOWN = new RequestTask<>(null, null);

  private BlockingQueue<RequestTask<T>> taskQueue = new LinkedBlockingDeque<>();

  private MessageProcessor<T> processor;

  public UdpDispatcher(MessageProcessor<T> processor) {
    this.processor = processor;
  }

  @Override
  public void run() {
    try {
      while (true) {
        RequestTask<T> unit = taskQueue.take();
        if (unit == EXECUTE_TASK_OR_SHUTDOWN) {
          LOGGER.info("shutdown thread : {}", Thread.currentThread());
          break;
        }
        processor.process(unit.session, unit.request);
        WriterBuffer writerBuffer = (WriterBuffer) unit.session.writeBuffer();
        writerBuffer.flush();
      }
    } catch (InterruptedException e) {
      LOGGER.error("interrupted exception :{}", e);
    }
  }

  /**
   * dispatch task.
   */
  public void dispatch(UdpAioSession<T> session,T request){
    dispatch(new RequestTask<>(session,request));
  }

  public void dispatch(RequestTask<T> requestTask){
    taskQueue.offer(requestTask);
  }

  class RequestTask<T> {
    UdpAioSession<T> session;
    T request;

    public RequestTask(UdpAioSession<T> session, T request) {
      this.session = session;
      this.request = request;
    }
  }
}
