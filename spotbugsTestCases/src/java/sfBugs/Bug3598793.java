package sfBugs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3598793 implements ServletResponse {

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

    @ExpectWarning(value="BC_UNCONFIRMED_CAST", confidence=Confidence.LOW)
    public void service2(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletResponse response;

        response = (HttpServletResponse) res;

        response.sendError(404);
    }

  @ExpectWarning("BC_UNCONFIRMED_CAST")
  public  Integer foo(Number x) {
      return (Integer) x;
  }

  @NoWarning("BC_UNCONFIRMED_CAST")
  public  Integer foo(Object x) {
      return (Integer) x;
  }
    
    @Override
    public void flushBuffer() throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getBufferSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Locale getLocale() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCommitted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resetBuffer() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBufferSize(int arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setCharacterEncoding(String arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setContentLength(int arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setContentType(String arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLocale(Locale arg0) {
        // TODO Auto-generated method stub
        
    }
    


}
