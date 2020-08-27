package xyz.vopen.framework.chaos.core.internals.event;

/**
 * {@link ChaosWatcher} A event which is create by Chaos.
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
public interface ChaosWatcher {

  void initializer(String cursor,Object obj);

  void started();

  void exit();
}
