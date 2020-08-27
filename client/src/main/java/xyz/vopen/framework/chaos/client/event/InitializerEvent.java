package xyz.vopen.framework.chaos.client.event;

import org.springframework.context.ApplicationEvent;

/**
 * {@link InitializerEvent}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/8/1
 */
public class InitializerEvent extends ApplicationEvent {

    public InitializerEvent(InitializerEventPair eventPair) {
        super(eventPair);
    }

    public InitializerEventPair getEventPair() {
        return (InitializerEventPair) source;
    }

    public static class InitializerEventPair{
        private String request;
        private Object obj;

        public InitializerEventPair(String request, Object obj) {
            this.request = request;
            this.obj = obj;
        }

        public String getRequest() {
            return request;
        }

        public Object getObj() {
            return obj;
        }
    }
}
