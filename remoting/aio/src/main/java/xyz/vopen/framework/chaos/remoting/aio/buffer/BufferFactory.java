package xyz.vopen.framework.chaos.remoting.aio.buffer;

/**
 * {@link BufferFactory}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public interface BufferFactory {

  /** 禁用状态的内存池 */
  BufferFactory DISABLED_BUFFER_FACTORY = () -> new BufferPagePool.NoneBufferPagePool();

  /**
   * 创建内存池
   *
   * @return
   */
  BufferPagePool create();
}
