package xyz.vopen.framework.chaos.common;

import java.net.URL;

/**
 * {@link AbstractLifeCycle}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/19
 */
public abstract class AbstractLifeCycle implements LifeCycle {


    public void preInit(){

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void destroy() {

    }

}
