package sfBugs;

import java.util.Map;

import javax.servlet.http.HttpSession;

public class Bug3053867  {

	  HttpSession session;
	  public void setSession(HttpSession session) {
		  this.session = session;
	  }
	  public void storeMap(Map<String, String> map) {
		  session.setAttribute("map", map);
	  }
	
	

}
