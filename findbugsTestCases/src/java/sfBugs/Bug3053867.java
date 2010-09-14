package sfBugs;

import java.util.Map;

import javax.servlet.http.HttpSession;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3053867 {

	static class Foo {
		int x;
	}

	HttpSession session;

	@NoWarning("J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION")
	public void setSession(HttpSession session) {
		this.session = session;
	}

	@NoWarning("J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION")
	public void storeMap(Map<String, String> map) {
		session.setAttribute("map", map);
	}

	@DesireWarning("J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION")
	public void storeMap(Foo foo) {
		session.setAttribute("foo", foo);
	}

}
