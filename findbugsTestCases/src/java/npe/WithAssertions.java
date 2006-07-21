package npe;

import java.util.HashMap;
import java.util.Map;

public class WithAssertions {
	Map<String, String> map = new HashMap<String,String>();
	int f(String s) {
		String result  = map.get(s);
		assert result != null : "Result shouldn't be null";
		// I think this should be reported
		return  result.hashCode();
	}

}
