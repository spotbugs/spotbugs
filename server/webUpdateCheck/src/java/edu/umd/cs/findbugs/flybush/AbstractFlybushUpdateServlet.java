package edu.umd.cs.findbugs.flybush;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

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