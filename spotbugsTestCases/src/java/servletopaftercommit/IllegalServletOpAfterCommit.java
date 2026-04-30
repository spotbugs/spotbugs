package servletopaftercommit;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

public class IllegalServletOpAfterCommit {

    // =========================================================================
    // NEGATIVE TEST CASES (COMPLIANT - Should not report a bug)
    // =========================================================================

    /**
     * 1. The official CERT FIO15-J Compliant example.
     */
    public void testCompliant(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        try {
            // Do work that doesn't require the output stream
        } catch (Exception x) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        ServletOutputStream out = response.getOutputStream();
        try {
            out.println("<html>");

            // ... All work

        } catch (IOException ex) {
            out.println(ex.getMessage());
        } finally {
            out.flush();
        }
    }

    /**
     * 2. Branches (Relaxed list test).
     */
    public void testCompliantMultipleSendErrorInBranches(HttpServletRequest request, HttpServletResponse response, boolean cond) throws IOException {
        if (cond) {
            response.sendError(400, "Bad Request"); // COMMITTED_UNACCESSED
        } else {
            response.sendError(404, "Not Found"); // COMMITTED_UNACCESSED
        }
    }

    /**
     * 3. Normal stream usage without errors.
     */
    public void testCompliantNormalWriterUsage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter(); // UNCOMMITTED_ACCESSED
        writer.write("Hello World");
        writer.close(); // COMMITTED_ACCESSED
        writer.close(); // COMMITTED_ACCESSED
    }

    /**
     * 4. Try-with-resources test.
     */
    public void testCompliantTryWithResources(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (ServletOutputStream out = response.getOutputStream()) { // UNCOMMITTED_ACCESSED
            out.print("<html>");
            out.flush(); // COMMITTED_ACCESSED
        }
    }

    /**
     * 5. Resetting before retrieving the stream is legal.
     */
    public void testCompliantResetBeforeStream(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.reset();
        ServletOutputStream out = response.getOutputStream(); // UNCOMMITTED_ACCESSED
        out.close(); // COMMITTED_ACCESSED
    }


    /**
     * 6. Generic Writer usage without Response parameter
     */
    public void testCompliantGenericWriterDoubleFlush(PrintWriter writer) {
        writer.flush(); // UNCOMMITTED_UNACCESSED
        writer.flush(); // UNCOMMITTED_UNACCESSED
    }


    /**
     * 7. Early return after a sendError in an if clause
     */
    public void testEarlyReturn(HttpServletResponse response, boolean flag) throws IOException {
        if(flag){
            response.sendError(500);
            return;
        }
        response.setHeader("X-Pass", "true");
    }

    // =========================================================================
    // POSITIVE TEST CASES (VIOLATING - Should report a bug)
    // =========================================================================

    /**
     * 1. The official CERT FIO15-J Non-Compliant first example (Illegal sendError in exception handling).
     */
    public void testNoncompliant1(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        ServletOutputStream out = response.getOutputStream();
        try {
            out.println("<html>");

            // ... Write some response text

            out.flush();  // Commits the stream

            // ... More work

        } catch (IOException x) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 2. The official CERT FIO15-J Non-Compliant second example (Double flush in exception handling).
     */
    public void testNoncompliant2(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        ServletOutputStream out = response.getOutputStream();
        try {
            out.println("<html>");

            // ... Write some response text

            out.flush();  // Commits the stream

            // ... More work

        } catch (IOException x) {
            out.println(x.getMessage());
            out.flush();
        }
    }



    /**
     * 3. Modifying header after sendError (Commit). (Relaxed list test)
     */
    public void testViolatingSetContentTypeAfterSendError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(500, "Server Error"); // COMMITTED_UNACCESSED
        response.setContentType("text/html"); // BUG: Illegal Servlet Output Operation After Commit
    }

    /**
     * 4. Calling sendRedirect after stream flush. (Strict list test)
     */
    public void testViolatingRedirectAfterFlush(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletOutputStream out = response.getOutputStream(); // UNCOMMITTED_ACCESSED
        out.flush(); // COMMITTED_ACCESSED
        response.sendRedirect("/login"); // BUG: Illegal Servlet Output Operation After Commit
    }

    /**
     * 5. Calling sendError after retrieving the stream.
     */
    public void testViolatingSendErrorAfterGetOutputStream(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletOutputStream out = response.getOutputStream(); // UNCOMMITTED_ACCESSED
        response.sendError(500); // BUG: sends error or redirects after initializing the output builder object
    }

    /**
     * 6. Nested Try-Finally block test.
     */
    public void testViolatingNestedFinallyDoubleFlush(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletOutputStream out = response.getOutputStream(); // UNCOMMITTED_ACCESSED
        try {
            try {
                out.write(1);
            } finally {
                out.flush(); // COMMITTED_ACCESSED
            }
        } finally {
            out.flush(); // BUG: Double flush
        }
    }

    /**
     * 7. Using flushBuffer() (has the same effect as stream.flush()).
     */
    public void testViolatingFlushBufferThenHeader(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.flushBuffer(); // COMMITTED_UNACCESSED
        response.setHeader("Custom-Header", "Value"); // BUG: Illegal Servlet Output Operation After Commit
    }

    /**
     * 8. Modifying setContentLength after stream.close().
     */
    public void testViolatingSetContentLengthAfterClose(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletOutputStream out = response.getOutputStream(); // UNCOMMITTED_ACCESSED
        out.write("Data".getBytes());
        out.close(); // COMMITTED_ACCESSED

        response.setContentLength(100); // BUG: Illegal Servlet Output Operation After Commit
    }

    /**
     * 9. Detecting double flush bug through a Wrapper object.
     */
    public void testViolatingWrapperDoubleFlush(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response);
        PrintWriter writer = wrapper.getWriter(); // UNCOMMITTED_ACCESSED
        writer.flush(); // COMMITTED_ACCESSED
        writer.flush(); // BUG: Illegal Servlet Output Operation After Commit
    }

    /**
     * 10. Custom Response and stream retrieval bug (isSubtypeSafe test).
     */
    public void testViolatingCustomTypes(MyAuditResponse customResponse) throws IOException {

        ServletOutputStream out = customResponse.getOutputStream(); // UNCOMMITTED_ACCESSED

        out.write("Data".getBytes());

        out.flush(); // COMMITTED_ACCESSED

        customResponse.setHeader("X-Secure", "true"); // BUG: Illegal Servlet Output Operation After Commit
    }

    /**
     * 11. Stream passed directly as a parameter.
     * Because ServletOutputStream is present in the signature, the method starts in UNCOMMITTED_ACCESSED.
     */
    public void testViolatingStreamAsParameter(ServletOutputStream out) throws IOException {
        out.flush(); // COMMITTED_ACCESSED
        out.flush(); // BUG: Illegal Servlet Output Operation After Commit
    }

    /**
     * 12. Generic Writer + Response passed as parameters (Context-sensitive heuristic test).
     * Because BOTH types are present, the detector recognizes the web context and starts in UNCOMMITTED_ACCESSED.
     */
    public void testViolatingGenericWriterWithResponseContext(HttpServletResponse response, PrintWriter writer) {
        writer.flush(); // COMMITTED_ACCESSED
        response.setHeader("X-Fail", "true"); // BUG: Illegal Servlet Output Operation After Commit
    }

    /**
     * 13. Testing various relaxed illegal methods to increase branch coverage.
     */
    public void testViolatingRelaxedMethodsForCoverage(HttpServletResponse response) throws IOException {
        response.flushBuffer(); // COMMITTED_UNACCESSED

        // BUG: All of these are illegal after commit
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setBufferSize(1024);
        response.reset();
        response.resetBuffer();
        response.addHeader("X-Header", "Value");
        response.addDateHeader("X-Date", 1234567890);
        response.addIntHeader("X-Int", 123);
        response.addCookie(new jakarta.servlet.http.Cookie("X-Cookie", "Value"));
        response.setHeader("X-Header", "Value");
        response.setDateHeader("X-Date", 1234567890);
        response.setIntHeader("X-Int", 123);
        response.setStatus(200);
        response.setContentLength(1024);
        response.setContentLengthLong(1024);
        response.setLocale(java.util.Locale.US);
    }

    /**
     * 14. Generic Writer + Custom Response passed as parameters (Context-sensitive heuristic test).
     */
    public void testViolatingGenericWriterWithCustomResponseContext(MyAuditResponse response, PrintWriter writer) {
        writer.flush(); // COMMITTED_ACCESSED
        response.setHeader("X-Fail", "true"); // BUG: Illegal Servlet Output Operation After Commit
    }
}