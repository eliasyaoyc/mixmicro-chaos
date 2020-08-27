package xyz.vopen.framework.chaos.common.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link LRUCache}
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/11
 */
public class LRUCache<K,V> implements Cache<K,V>{

    private final LinkedHashMap<K,V> cache;

    public LRUCache(final int maxSize) {
        this.cache = new LinkedHashMap<K,V>(16,.75f,true){
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return this.size() > maxSize;
            }
        };
    }

    @Override
    public V get(K key) {
        return this.cache.get(key);
    }

    @Override
    public void put(K key, V value) {
        this.cache.put(key,value);
    }

    @Override
    public boolean remove(K key) {
        return this.cache.remove(key) != null;
    }

    @Override
    public long size() {
        return this.cache.size();
    }
}
