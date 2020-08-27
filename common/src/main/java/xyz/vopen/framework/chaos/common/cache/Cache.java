package xyz.vopen.framework.chaos.common.cache;

/**
 * {@link Cache}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
public interface Cache<K,V> {

    V get(K key);

    void put(K key,V value);

    boolean remove(K key);

    long size();
}
