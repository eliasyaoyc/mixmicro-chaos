package xyz.vopen.framework.chaos.client.event;

import org.springframework.context.ApplicationEvent;

/**
 * {@link StartedEvent}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/8/1
 */
public class StartedEvent extends ApplicationEvent {
    public StartedEvent(Object source) {
        super(source);
    }
}
