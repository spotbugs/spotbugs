
import java.util.*;

class BadCast {

	Object foo() {
		return new Hashtable();
		}
	Map bar() {
		return new Hashtable();
		}

	int f() {
		return ((Hashtable)foo()).size();
		}
	int h() {
		return ((Hashtable)bar()).size();
		}
	int g() {
		return ((Hashtable[])foo()).length;
		}
	}

	
