package bugIdeas;

import java.util.HashMap;

public class Ideas_2008_12_02 {
	
	HashMap<String,String> map = new HashMap<String,String>();
	HashMap<Object,Object> map2 = new HashMap<Object,Object>();
	
	public void test1() {
		for(String s : map.keySet())
			if (map2.containsKey(s))
				System.out.println("Found intersection");
	}
	public void test2() {
		for(Object o : map2.keySet())
			if (map.containsKey(o))
				System.out.println("Found intersection");
	}
}
