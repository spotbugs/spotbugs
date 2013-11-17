package edu.umd.cs.findbugs.flybush;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

public abstract class AbstractFlybushCloudServlet extends AbstractFlybushServlet<PersistenceHelper> {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String helperCls = config.getServletContext().getInitParameter("edu.umd.cs.findbugs.flybush.cloudPersistenceHelper");
        try {
            persistenceHelper = (PersistenceHelper) Class.forName(helperCls).newInstance();
        } catch (Exception e) {
            throw new ServletException("Couldn't load persistence helper " + helperCls, e);
        }
    }
    protected String getCloudName() {
        return jspHelper.getCloudName();
    }

    
   
    @SuppressWarnings("unchecked")
    protected SqlCloudSession lookupCloudSessionById(long id, PersistenceManager pm) {
        Query query = pm.newQuery("select from " + persistenceHelper.getSqlCloudSessionClassname()
                + " where randomID == :randomIDquery");
        List<SqlCloudSession> sessions = (List<SqlCloudSession>) query.execute(Long.toString(id));
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    protected static LinkedList<DbEvaluation> sortAndFilterEvaluations(Set<? extends DbEvaluation> origEvaluations) {
        Set<String> seenUsernames = new HashSet<String>();
        List<DbEvaluation> evaluationsList = new ArrayList<DbEvaluation>(origEvaluations);
        Collections.sort(evaluationsList);
        int numEvaluations = evaluationsList.size();
        LinkedList<DbEvaluation> result = new LinkedList<DbEvaluation>();
        for (ListIterator<DbEvaluation> it = evaluationsList.listIterator(numEvaluations); it.hasPrevious();) {
            DbEvaluation dbEvaluation = it.previous();
            boolean userIsNew = seenUsernames.add(dbEvaluation.getWhoId());
            if (userIsNew) {
                result.add(0, dbEvaluation);
            }
        }
        return result;
    }
    
}
