package implementation;

import dao.DiskAccess;
import dao.KeyValue;
import services.Service;
import util.CacheEvictionUtility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a service to save overflowing cache entries to disk using filesystem.
 */
public class DiskCacheService implements Service {
    private DiskAccess dao;
    private CacheEvictionUtility evictUtil;

    private Map<String, Object> cache;
    private Logger log = Logger.getLogger("DiskCacheService");
    private int maxSize;

    public DiskCacheService(int maxSize, DiskAccess dao, CacheEvictionUtility evictUtil){
        this.maxSize = maxSize;
        cache = new HashMap<>(maxSize);
        this.evictUtil = evictUtil;
        this.dao = dao;
    }

    /**
     * Puts a key in the map with the given value.
     * <br/>
     * If the key is already on the map, does not change its value.
     * <br/>
     * If map is full, makes space on the cache by removing
     * an existing key as defined by eviction policy and inserts key on the cache.
     * Removed key from cache then gets stored on file.
     * <br/>
     * If key is on file, it is removed from file.
     * @param key
     * @param value
     */
    @Override
    public synchronized void put(String key, Object value) {
        log.log(Level.FINE, "caching key - " + key);
        if(key == null){
            return;
        }
        //cache has space or already has key cached
        if(cache.size() < maxSize || cache.containsKey(key)){
            evictUtil.put(key);
            cache.putIfAbsent(key, value);
            //remove key from file on disk if present
            dao.remove(key);
            return;
        }
        //key not in the cache
        //make space for the new key on the cache
        String evictedKey = evictUtil.remove();
        Object evictedValue = cache.get(evictedKey);
        cache.remove(evictedKey);
        evictUtil.put(key);
        try{
            //put key removed from cache in filestorage
            dao.put(evictedKey, evictedValue);
            //see if current key is in file if so, remove it
            dao.remove(key);
            cache.put(key, value);
        }
        catch (Exception e){
            log.log(Level.SEVERE, "Exception accessing file storage");
        }
    }

    /**
     * Gets a given key from cache.
     * <br/>
     * If key is on cache, returns it
     * <br/>
     * If key is on disk, brings it to the cache before returning.
     * Otherwise, returns null.
     * Removes the key from disk.
     * <br/> If queue is full, removes a key from cache as defined by the eviction policy
     * and stores that key to disk before inserting.
     * @param key to get
     * @return the Object found or null
     */
    @Override
    public synchronized Object get(String key){
        //key in cache? great! return it
        if(key == null || cache.containsKey(key)){
            return cache.getOrDefault(key, null);
        }
        //key not in cache
        //key in file?
        log.log(Level.FINE, "attempting to get key from disk - " + key);
        KeyValue item = dao.remove(key);
        if(item == null){
            //key not in the file either
            return null;
        }
        //item in file
        //does map have space?
        if(cache.size() < maxSize){
            //yes, thats great, put the item on map
            cache.put(item.getKey(), item.getValue());
            evictUtil.put(item.getKey());
            return item.getValue();
        }
        //map is full :(
        String evictedKey = evictUtil.remove();
        Object evictedValue = cache.get(evictedKey);
        cache.remove(evictedKey);
        try {
            log.log(Level.FINE, "persisting key on disk - " + evictedKey);
            //put key removed from cache in filestorage
            dao.put(evictedKey, evictedValue);
        }
        catch (Exception e){
            log.log(Level.SEVERE, "Exception accessing file storage");
            return null;
        }
        cache.put(item.getKey(), item.getValue());
        evictUtil.put(item.getKey());
        return item.getValue();
    }

    /**
     * Gets the current version of the map
     * @return HashMap
     */
    public Map getMapSnapshot(){
        return Collections.unmodifiableMap(cache);
    }

    /**
     * Clean up everything
     */
    private void clear(){
        cache.clear();
        dao.clear();
        evictUtil.clear();
    }

}
