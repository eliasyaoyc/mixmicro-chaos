package xyz.vopen.framework.chaos.remoting.api;

import java.nio.ByteBuffer;

/**
 * {@link Decoder}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public interface Decoder {

    /**
     * decode algorithm.
     *
     * @param byteBuffer
     * @return
     */
    boolean decode(ByteBuffer byteBuffer);

    /**
     * Return thw complete data parsed this time.
     *
     * @return
     */
    ByteBuffer getBuffer();
}
