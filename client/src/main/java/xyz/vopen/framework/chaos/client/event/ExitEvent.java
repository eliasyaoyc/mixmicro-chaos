package xyz.vopen.framework.chaos.client.event;

import org.springframework.context.ApplicationEvent;

/**
 * {@link ExitEvent}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/8/1
 */
public class ExitEvent extends ApplicationEvent {

    public ExitEvent(Object source) {
        super(source);
    }
}
