package xyz.vopen.framework.chaos.common.spi;

/**
 * {@link ChaosInternalLoadingStrategy}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
public class ChaosInternalLoadingStrategy implements LoadingStrategy{

    private static final String DIRECTORY = "META-INF/chaos/internal";

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
