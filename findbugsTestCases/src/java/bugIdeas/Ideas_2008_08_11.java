package bugIdeas;

import java.util.HashMap;

import junit.framework.TestCase;

import org.junit.Test;

public class Ideas_2008_08_11  extends TestCase {
	
	@Test
    public void fooBar() throws Exception {
		assertFalse(Boolean.TRUE.equals(5));
		HashMap<String,Integer> map = new HashMap<String,Integer>();
    	map.put("a", 1);
		assertFalse(map.containsKey(1));
		assertNull(map.get(1));
    }
	
	
    public void testFoo() throws Exception {
    	assertFalse(Boolean.TRUE.equals(5));
    	HashMap<String,Integer> map = new HashMap<String,Integer>();
    	map.put("a", 1);
		assertFalse(map.containsKey(1));
		assertNull(map.get(1));
    }

}
