package sfBugs;

public class Bug1723940 {
	
	Bug1723940 child;
	Bug1723940 getChild() {
		return child;
	}
	static public void doX(Bug1723940 o) {
	    o.hashCode();

	    while(o != null) {
	        o = o.getChild();
	    }

	}


}
