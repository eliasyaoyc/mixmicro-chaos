package xyz.vopen.framework.chaos.core.internals;

import xyz.vopen.framework.chaos.remoting.api.Request;

import java.nio.ByteBuffer;

/**
 * {@link Serializer}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/13
 */
public interface Serializer<T> {

    /** do serializer. */
    T serializer(final Request request);

    /** do deserializer. */
    T deserializer(final ByteBuffer readBuffer);

    /**
     * Return the type of protocol {@link SerializerType}.
     *
     * @return the type of protocol.
     */
    SerializerType getSerializationType();


    enum SerializerType {
        JSON(1),

        HESSIAN(2);

        private int type;

        SerializerType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }
}
