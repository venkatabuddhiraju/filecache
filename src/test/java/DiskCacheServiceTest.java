import dao.DiskAccess;
import dao.KeyValue;
import implementation.DiskCacheService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import util.CacheEvictionUtility;

import java.util.Map;

public class DiskCacheServiceTest {
    private static CacheEvictionUtility evictUtility = mock(CacheEvictionUtility.class);
    private static DiskAccess dao = mock(DiskAccess.class);
    private static DiskCacheService service = new DiskCacheService(5, dao, evictUtility);

    @BeforeAll
    static void setup() {
        service.put("one", 1);
        service.put("two", 12);
        service.put("three", 13);
        service.put("four", 14);
        service.put("five", 15);
    }

    @Test
    public void noChangeToOriginalMap(){
        Assertions.assertEquals( 5 , service.getMapSnapshot().size());
    }

    @Test
    public void addOneElementToFullMapSize(){
        when(evictUtility.remove()).thenReturn("two");
        service.put("six", 16);
        Assertions.assertEquals( 5 , service.getMapSnapshot().size());
    }

    @Test
    public void getValueFromFullMapSize(){
        when(evictUtility.remove()).thenReturn("one");
        when(dao.remove("ten")).thenReturn(new KeyValue("ten", "103"));
        service.get("ten");
        Assertions.assertEquals( 5 , service.getMapSnapshot().size());
    }

    @Test
    public void getValueFromFullMapRemovedKey(){
        when(evictUtility.remove()).thenReturn("one");
        when(dao.remove("ten")).thenReturn(new KeyValue("ten", "103"));
        service.get("ten");
        Assertions.assertEquals( 5 , service.getMapSnapshot().size());
        Assertions.assertNull(service.get("one"));
    }

    @Test
    public void getValueFromFullMapAddedKey(){
        when(evictUtility.remove()).thenReturn("one");
        when(dao.remove("ten")).thenReturn(new KeyValue("ten", "103"));
        service.get("ten");
        Assertions.assertEquals( 5 , service.getMapSnapshot().size());
        Assertions.assertEquals(103, Integer.parseInt((String)service.getMapSnapshot().get("ten")));
    }

    @Test
    public void getValueFromNonFullMap(){
        DiskCacheService service2 = new DiskCacheService(5, dao, evictUtility);
        service2.put("one", 1);
        service2.put("two", 12);
        service2.put("three", 13);
        service2.put("four", 14);
        when(evictUtility.remove()).thenReturn("one");
        when(dao.remove("ten")).thenReturn(new KeyValue("ten", 103));
        service2.get("ten");
        Assertions.assertEquals( 5 , service2.getMapSnapshot().size());
        Assertions.assertEquals(103, (Object)service2.getMapSnapshot().get("ten"));
    }

    @Test
    public void putValueOnNonFullMap(){
        DiskCacheService service2 = new DiskCacheService(5, dao, evictUtility);
        service2.put("one", 1);
        service2.put("two", 12);
        service2.put("three", 13);
        service2.put("four", 14);
        when(evictUtility.remove()).thenReturn("one");
        when(dao.remove("ten")).thenReturn(new KeyValue("ten", "103"));
        service2.put("ten", 103);
        Assertions.assertEquals( 5 , service2.getMapSnapshot().size());
        Assertions.assertEquals(103, (Object)service2.getMapSnapshot().get("ten"));
    }

    @Test
    public void getNonExistentValue(){
        when(evictUtility.remove()).thenReturn("one");
        when(dao.remove("ten")).thenReturn(new KeyValue("ten", "103"));
        Object value = service.get("yellow");
        Assertions.assertNull(value);
    }

    @Test
    public void putNullKey(){
        when(evictUtility.remove()).thenReturn("one");
        when(dao.remove("ten")).thenReturn(new KeyValue("ten", "103"));
        Assertions.assertEquals( 5 , service.getMapSnapshot().size());
        service.put(null, "whatever");
        //No change to map
        Assertions.assertEquals( 5 , service.getMapSnapshot().size());
    }

    @Test
    public void putExistingKeyNewValue(){
        Assertions.assertEquals( 13 , service.getMapSnapshot().get("three"));
        service.put("three", 10);
        //No change to map
        Assertions.assertEquals( 13 , service.getMapSnapshot().get("three"));
    }

    @Test
    public void getNullKey(){
        Object o = service.get(null);
        Assertions.assertNull(o);
    }


}
