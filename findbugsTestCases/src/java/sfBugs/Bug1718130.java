package sfBugs;

import org.apache.tapestry.spring.SpringBean;

import com.google.inject.Inject;

/**
 * With annotations and inversion of control being all the rage these days, a
 * new pattern seems to be gaining popularity: annotating class fields with
 * something like '@SpringBean' (wicket/spring) or '@Inject' (Google Guice),
 * and somehow have them instantiated by reflection by the framework.
 */

public class Bug1718130 {
	@Inject Object x;
	@SpringBean Object y;
	
	public int hashCode() {
		return x.hashCode() + y.hashCode();
	}

}
