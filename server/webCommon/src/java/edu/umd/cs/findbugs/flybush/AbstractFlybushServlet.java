package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractFlybushServlet<PersistenceHelper extends BasePersistenceHelper>
		extends HttpServlet {

	protected static final Logger LOGGER = Logger.getLogger(AbstractFlybushServlet.class.getName());
	protected PersistenceHelper persistenceHelper;
	protected JspHelper jspHelper = new JspHelper();

	public AbstractFlybushServlet() {
		super();
	}


    protected String getInitParameter(ServletConfig config, String name) {
        ServletContext servletContext = config.getServletContext();
        
        String result = servletContext.getInitParameter(name);
        if (result != null) 
            return result;
        System.out.println("Did not find init parameter " + result);
        for(Enumeration<String> e = servletContext.getInitParameterNames(); e.hasMoreElements(); ) {
            String s = e.nextElement();
            System.out.println(s);
            
        }
        throw new RuntimeException("could not find " + name);
    }
    
	/** for testing */
	protected void setPersistenceHelper(PersistenceHelper persistenceHelper) {
	    this.persistenceHelper = persistenceHelper;
	}

	/** Change visibility to public */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
			    super.doGet(req, resp);
			}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	    String uri = req.getRequestURI();
	
	    PersistenceManager pm = getPersistenceManager();
	
	    try {
	        handlePost(pm, req, resp, uri);
	    } finally {
	        pm.close();
	    }
	}

	protected abstract void handlePost(PersistenceManager pm, HttpServletRequest req,
			HttpServletResponse resp, String uri) throws IOException;

	protected void show404(HttpServletResponse resp) throws IOException {
	    setResponse(resp, 404, "Not Found");
	}

	protected void setResponse(HttpServletResponse resp, int statusCode, String textResponse)
			throws IOException {
			    resp.setStatus(statusCode);
			    resp.setContentType("text/plain");
			    resp.getWriter().println(textResponse);
			}

	protected PersistenceManager getPersistenceManager() throws IOException {
	    return persistenceHelper.getPersistenceManager();
	}

	/** for testing */
	protected long getCurrentTimeMillis() {
	    return System.currentTimeMillis();
	}

    protected String getCloudName() {
        return jspHelper.getCloudName();
    }
}