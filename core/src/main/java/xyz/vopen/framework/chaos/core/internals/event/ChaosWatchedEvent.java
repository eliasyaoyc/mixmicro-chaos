package xyz.vopen.framework.chaos.core.internals.event;

import xyz.vopen.framework.chaos.core.internals.request.ChaosRequestFactory.*;
import xyz.vopen.framework.chaos.remoting.api.StateMachineEnum;

/**
 * {@link ChaosWatchedEvent} Represents a change on the zookeeper that a {@link ChaosWatcher} is
 * able to response to. The {@link ChaosWatchedEvent} includes exactly what happened, the current
 * state of the {@link StateMachineEnum}, and the chaos node that was involved in the event.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/17
 */
public class ChaosWatchedEvent {

  private StateMachineEnum stateMachineEnum;

  private Type eventType;

  /**
   * Create a ChaosWatchedEvent with specified type, stat.
   *
   * @param stateMachineEnum
   * @param eventType
   */
  public ChaosWatchedEvent(StateMachineEnum stateMachineEnum, Type eventType) {
    this.stateMachineEnum = stateMachineEnum;
    this.eventType = eventType;
  }

  public ChaosWatchedEvent(ChaosWatchedEvent event) {
    this.stateMachineEnum = event.getStateMachineEnum();
    this.eventType = event.eventType;
  }

  public StateMachineEnum getStateMachineEnum() {
    return stateMachineEnum;
  }

  public void setStateMachineEnum(StateMachineEnum stateMachineEnum) {
    this.stateMachineEnum = stateMachineEnum;
  }

  public Type getEventType() {
    return eventType;
  }

  public void setEventType(Type eventType) {
    this.eventType = eventType;
  }

  /**
   * Convert ChaosWatchEvent to type that can be sent over network.
   *
   * @return ChaosWatchedEvent
   */
  public ChaosWatchedEvent getWrapper() {
    return new ChaosWatchedEvent(stateMachineEnum, eventType);
  }
}
