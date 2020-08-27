package xyz.vopen.framework.chaos.core.internals;

import xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager;
import xyz.vopen.framework.chaos.core.internals.request.ChaosRequest;
import xyz.vopen.framework.chaos.core.internals.request.ChaosResponse;
import xyz.vopen.framework.chaos.remoting.api.Session;

import java.util.Map;
import java.util.Queue;

/**
 * {@link EventManager}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
public interface EventManager {

  Queue<ChaosEventManager.ChaosEvent> materialize(ChaosRequest request, Session session);

  void add(ChaosRequest request, Session session);

  void removeEvent(ChaosRequest request, Session session);

  void update(ChaosRequest request,Session session);

  void confirm(ChaosRequest response, Session session);

  Map<Session, ChaosEventManager.SessionPair> getSessions();

  void initialize();

  void destroy();

  ChaosEventManager.ChaosRequestPair getResponse(boolean isSyncQueue);

  void addUnreachableNode(Session session);

  void addServiceNode(Session session);

  public Session getSession(String sessionId);

  void syncCallback(ChaosResponse chaosResponse,Session session);
}
