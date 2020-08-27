package xyz.vopen.framework.chaos.remoting.api;

/**
 * {@link Group}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public interface Group {

  void join(String group, Session session);

  void remove(String group, Session session);

  void remove(Session session);

  void writeToGroup(String group, byte[] t);
}
