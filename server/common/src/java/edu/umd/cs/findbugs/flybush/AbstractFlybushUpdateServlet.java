package edu.umd.cs.findbugs.flybush;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import edu.umd.cs.findbugs.flybush.AbstractFlybushServlet;
import edu.umd.cs.findbugs.flybush.BasePersistenceHelper;

public abstract class AbstractFlybushUpdateServlet extends AbstractFlybushServlet<UsagePersistenceHelper> {

    public AbstractFlybushUpdateServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String helperCls = getInitParameter(config, "edu.umd.cs.findbugs.flybush.usagePersistenceHelper");
        try {
            persistenceHelper = (UsagePersistenceHelper) Class.forName(helperCls).newInstance();
        } catch (Exception e) {
            throw new ServletException("Couldn't load persistence helper " + helperCls, e);
        }
    }

    

}