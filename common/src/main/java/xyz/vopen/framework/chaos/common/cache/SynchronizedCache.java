package xyz.vopen.framework.chaos.common.cache;

/**
 * {@link SynchronizedCache}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
public class SynchronizedCache<K,V> implements Cache<K,V>{

    private final Cache<K,V> underlying;

    public SynchronizedCache(Cache<K, V> underlying) {
        this.underlying = underlying;
    }

    @Override
    public synchronized V get(K key) {
        return this.underlying.get(key);
    }

    @Override
    public synchronized void put(K key, V value) {
        this.underlying.put(key,value);
    }

    @Override
    public synchronized boolean remove(K key) {
        return this.underlying.remove(key);
    }

    @Override
    public synchronized long size() {
        return this.underlying.size();
    }
}
