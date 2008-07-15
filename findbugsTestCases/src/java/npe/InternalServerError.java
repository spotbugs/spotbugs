package npe;

import javax.servlet.http.HttpServletResponse;

public class InternalServerError {

	static int foo(Object x, HttpServletResponse resp) {
		if (x == null)
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		return x.hashCode();
	}
}
