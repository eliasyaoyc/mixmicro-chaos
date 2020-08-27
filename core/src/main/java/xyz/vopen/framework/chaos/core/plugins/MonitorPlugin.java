package xyz.vopen.framework.chaos.core.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.vopen.framework.chaos.common.utilities.TimerTaskUtility;
import xyz.vopen.framework.chaos.remoting.api.AbstractPlugin;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * {@link MonitorPlugin}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class MonitorPlugin<T> extends AbstractPlugin<T> implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(MonitorPlugin.class);

  /** 任务执行频率 */
  private int seconds = 0;
  /** 当前周期内消息 流量监控 */
  private LongAdder inFlow = new LongAdder();

  /** 当前周期内消息 流量监控 */
  private LongAdder outFlow = new LongAdder();

  /** 当前周期内处理失败消息数 */
  private LongAdder processFailNum = new LongAdder();

  /** 当前周期内处理消息数 */
  private LongAdder processMsgNum = new LongAdder();

  private LongAdder totalProcessMsgNum = new LongAdder();

  /** 新建连接数 */
  private LongAdder newConnect = new LongAdder();

  /** 断链数 */
  private LongAdder disConnect = new LongAdder();

  /** 在线连接数 */
  private long onlineCount;

  private LongAdder totalConnect = new LongAdder();

  private LongAdder readCount = new LongAdder();

  private LongAdder writeCount = new LongAdder();

  public MonitorPlugin() {
    this(60);
  }

  public MonitorPlugin(int seconds) {
    this.seconds = seconds;
    long mills = TimeUnit.SECONDS.toMillis(seconds);
    TimerTaskUtility.scheduleAtFixedRate(this, mills, mills);
  }

  @Override
  public boolean preProcess(Session session, T t) {
    processMsgNum.increment();
    totalProcessMsgNum.increment();
    return true;
  }

  @Override
  public void stateEvent(
      StateMachineEnum stateMachineEnum, Session session, Throwable throwable) {
    switch (stateMachineEnum) {
      case PROCESS_EXCEPTION:
        processFailNum.increment();
        break;
      case NEW_SESSION:
        newConnect.increment();
        break;
      case SESSION_CLOSED:
        disConnect.increment();
        break;
      default:
        // ignore other state
        break;
    }
  }

  @Override
  public void run() {
    long curInFlow = getAndReset(inFlow);
    long curOutFlow = getAndReset(outFlow);
    long curDiscardNum = getAndReset(processFailNum);
    long curProcessMsgNum = getAndReset(processMsgNum);
    long connectCount = getAndReset(newConnect);
    long disConnectCount = getAndReset(disConnect);
    onlineCount += connectCount - disConnectCount;
    LOGGER.info(
        "\r\n-----"
            + seconds
            + "seconds ----\r\ninflow:\t\t"
            + curInFlow * 1.0 / (1024 * 1024)
            + "(MB)"
            + "\r\noutflow:\t"
            + curOutFlow * 1.0 / (1024 * 1024)
            + "(MB)"
            + "\r\nprocess fail:\t"
            + curDiscardNum
            + "\r\nprocess success:\t"
            + curProcessMsgNum
            + "\r\nprocess total:\t"
            + totalProcessMsgNum.longValue()
            + "\r\nread count:\t"
            + getAndReset(readCount)
            + "\twrite count:\t"
            + getAndReset(writeCount)
            + "\r\nconnect count:\t"
            + connectCount
            + "\r\ndisconnect count:\t"
            + disConnectCount
            + "\r\nonline count:\t"
            + onlineCount
            + "\r\nconnected total:\t"
            + getAndReset(totalConnect)
            + "\r\nRequests/sec:\t"
            + curProcessMsgNum * 1.0 / seconds
            + "\r\nTransfer/sec:\t"
            + (curInFlow * 1.0 / (1024 * 1024) / seconds)
            + "(MB)");
  }

  private long getAndReset(LongAdder longAdder) {
    long result = longAdder.longValue();
    longAdder.add(-result);
    return result;
  }

  @Override
  public void afterRead(Session session, int readSize) {
    // 出现result为0,说明代码存在问题
    if (readSize == 0) {
      LOGGER.error("readSize is 0");
    }
    inFlow.add(readSize);
  }

  @Override
  public void beforeRead(Session session) {
    readCount.increment();
  }

  @Override
  public void afterWrite(Session session, int writeSize) {
    outFlow.add(writeSize);
  }

  @Override
  public void beforeWrite(Session session) {
    writeCount.increment();
  }
}
