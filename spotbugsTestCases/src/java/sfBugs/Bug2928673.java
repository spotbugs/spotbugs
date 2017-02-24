package sfBugs;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class Bug2928673 {
    static final Logger LOGGER = Logger.getAnonymousLogger();

    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            final PrintWriter out = response.getWriter();
            final HttpServletResponse wrapper = (HttpServletResponse) response;
            chain.doFilter(request, wrapper);
            final String origData = wrapper.getContentType();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Hello");
            }
            if ("text/html".equals(wrapper.getContentType())) {
                final CharArrayWriter caw = new CharArrayWriter();
                final int bodyIndex = origData.indexOf("</body>");
                if (-1 != bodyIndex) {
                    caw.write(origData.substring(0, bodyIndex - 1));
                    caw.write("\n<p>My custom footer</p>");
                    caw.write("\n</body></html>");
                    response.setContentLength(caw.toString().length());
                    out.write(caw.toString());
                } else {
                    out.write(origData);
                }
            } else {
                out.write(origData);
            }
            out.close();
        } else {
            chain.doFilter(request, response);
        }
    }

}
