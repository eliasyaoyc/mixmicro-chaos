package xyz.vopen.framework.chaos.common.spi;

/**
 * {@link ChaosLoadingStrategy}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
public class ChaosLoadingStrategy implements LoadingStrategy {

  private static final String DIRECTORY = "META-INF/chaos/";

  @Override
  public String directory() {
    return DIRECTORY;
  }

  @Override
  public boolean overridden() {
    return true;
  }

  @Override
  public int getPriority() {
    return NORMAL_PRIORITY;
  }
}
