package util;


/**
 * Decides the eviction policy for the cache <br/>
 * Could be lru, fifo, lifo etc.
 */
//ideally implements an interface so we could have several eviction policies
public class CacheEvictionUtility {
    //some data structure to hold keys to evict
    //stack or queue or map etc depending on the policy

    public void put(String key) {
    }

    public String remove() {
        //return "one2";
        return null;
    }

    public void clear() {
    }
}
