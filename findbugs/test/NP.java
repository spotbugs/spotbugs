import java.util.*;

class NP {

  int x;
  static Map m = new HashMap();
  public static void main(String args[]) {
	NP n = (NP) m.get("Foo");

	if (n != null) 
		System.out.println(n.x);
	}
}
	
