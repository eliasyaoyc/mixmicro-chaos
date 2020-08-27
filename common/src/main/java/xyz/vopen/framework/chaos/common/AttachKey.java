package xyz.vopen.framework.chaos.common;

import xyz.vopen.framework.chaos.common.exception.ChaosException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link AttachKey}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/10
 */
public class AttachKey<T> {

  /** attachment upper limit. */
  public static final int MAX_ATTACH_COUNT = 128;

  /** the cache for same key. */
  private static final Map<String, AttachKey> NAMES = new ConcurrentHashMap<>();

  /** index constructor{@link #index}. */
  private static final AtomicInteger INDEX_BUILDER = new AtomicInteger(0);

  /** the name of attachment. */
  private final String key;

  /** the index of attachment. */
  private final int index;

  public AttachKey(String key) {
    this.key = key;
    this.index = INDEX_BUILDER.getAndIncrement();
    if (this.index < 0 || this.index >= MAX_ATTACH_COUNT) {
      throw new ChaosException("too many attach key, permit number : " + MAX_ATTACH_COUNT);
    }
  }

  public static <T> AttachKey<T> valueOf(String name) {
    AttachKey<T> option = NAMES.get(name);
    if (option == null) {
      option = new AttachKey<>(name);
      AttachKey<T> old = NAMES.putIfAbsent(name, option);
      if (old != null) {
        option = old;
      }
    }
    return option;
  }

  public String getKey() {
    return this.key;
  }

  public int getIndex() {
    return this.index;
  }
}
