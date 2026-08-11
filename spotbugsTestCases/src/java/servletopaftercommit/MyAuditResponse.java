package servletopaftercommit;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public abstract class MyAuditResponse extends HttpServletResponseWrapper{
    // =========================================================================
    // CUSTOM IMPLEMENTATION FOR TESTING
    // =========================================================================

    public MyAuditResponse(HttpServletResponse response) {
        super(response);
    }
}
