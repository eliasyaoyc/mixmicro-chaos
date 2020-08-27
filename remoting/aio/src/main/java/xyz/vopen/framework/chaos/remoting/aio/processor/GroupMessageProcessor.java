package xyz.vopen.framework.chaos.remoting.aio.processor;

import xyz.vopen.framework.chaos.remoting.aio.transport.WriterBuffer;
import xyz.vopen.framework.chaos.remoting.api.AbstractAioSession;
import xyz.vopen.framework.chaos.remoting.api.Buffer;
import xyz.vopen.framework.chaos.remoting.api.Group;
import xyz.vopen.framework.chaos.remoting.api.MessageProcessor;
import xyz.vopen.framework.chaos.remoting.api.Session;
import xyz.vopen.framework.chaos.remoting.api.exception.RemotingException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link GroupMessageProcessor}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public abstract class GroupMessageProcessor<T> implements MessageProcessor<T>, Group {

  private Map<String, GroupUnit> sessionGroup = new ConcurrentHashMap<>();

  @Override
  public final synchronized void join(String group, Session session) {
    GroupUnit groupUnit = sessionGroup.get(group);
    if (groupUnit == null) {
      groupUnit = new GroupUnit();
      sessionGroup.put(group, groupUnit);
    }
    groupUnit.groupList.add(session);
  }

  @Override
  public void remove(String group, Session session) {
    GroupUnit groupUnit = sessionGroup.get(group);
    if (groupUnit == null) {
      return;
    }
    groupUnit.groupList.remove(session);
    if (groupUnit.groupList.isEmpty()) {
      sessionGroup.remove(group);
    }
  }

  @Override
  public void remove(Session session) {
    for (String group : sessionGroup.keySet()) {
      remove(group, session);
    }
  }

  @Override
  public void writeToGroup(String group, byte[] t) {
    GroupUnit groupUnit = sessionGroup.get(group);
    for (Session tSession : groupUnit.groupList) {
      if (!(tSession instanceof AbstractAioSession)) {
        throw new ClassCastException();
      }
      try {
        AbstractAioSession aioSession = (AbstractAioSession) tSession;
        WriterBuffer buffer = (WriterBuffer) aioSession.writeBuffer();
        buffer.write(t);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private class GroupUnit {
    Set<Session> groupList = new HashSet<>();
  }
}
