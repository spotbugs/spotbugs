package npe;

public class GuaranteedDereference {
	
	int f(Object x, boolean b) {
		int result = 0;
		if (x == null) result = 42;
		if (b) result++;
		result += x.hashCode();
		return result;
	}
	int g(Object x, boolean b, boolean b2) {
		int result = 0;
		if (x == null) result = 42;
		if (b) result++;
		if (b2) result += x.hashCode();
		else result -= x.hashCode();
		return result;
	}
	
	String h(Object x) {
		String value = null;
		StringBuffer result = new StringBuffer();
		String xAsString = null;
		if (x instanceof String) 
		  xAsString = (String) x;
		
		result.append(x.hashCode());
		
		if (xAsString != null)
			value = xAsString.toLowerCase();
		
		
		if (value == null) 
			result.append(value);
		else result.append("foo");
		
		result.append(" bar ");
		
		result.append(xAsString.trim());
		return result.toString();
	}

}
