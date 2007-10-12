import java.util.concurrent.ConcurrentHashMap;

class CHM {

	ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();

	int getLength(String s) {
		return map.get(s).length();
	}

	void put(String k, String v) {
		if (k == null)
			map.put(k, v); // should warn here about k being null
	}
}
