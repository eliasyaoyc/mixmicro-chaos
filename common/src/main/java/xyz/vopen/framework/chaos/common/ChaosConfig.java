package xyz.vopen.framework.chaos.common;

import org.apache.logging.log4j.core.net.Protocol;

/**
 * {@link ChaosConfig}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/26
 */
public interface ChaosConfig {

    String getServiceName();

    String getClientId();

    long getRequestTimeout();

    boolean isCoordinator();

    String getServices();

    int getSerializerType();
}
