package sfBugsNew;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1150 {
    @NoWarning("BC_UNCONFIRMED_CAST")
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletResponse response;

        try {
            response = (HttpServletResponse) res;
        } catch (ClassCastException e) {
            throw new ServletException("non-HTTP request or response", e);
        }
        response.sendError(404);
    }

    public Integer test() {
        return getI(1.2) + getI2(1.3);
    }
    
    @ExpectWarning(value="BC_UNCONFIRMED_CAST", confidence=Confidence.LOW)
    public Integer getI(Number o) {
        return (Integer) o;
    }

    @NoWarning("BC_UNCONFIRMED_CAST")
    public Integer getI2(Number o) {
        try {
            return (Integer) o;
        } catch (ClassCastException e) {
            return 42;
        }
    }
}
