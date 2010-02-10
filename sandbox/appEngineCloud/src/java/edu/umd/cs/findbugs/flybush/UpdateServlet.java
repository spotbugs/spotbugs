package edu.umd.cs.findbugs.flybush;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import org.datanucleus.store.query.QueryResult;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class UpdateServlet extends AbstractFlybushServlet {

    private static final Logger LOGGER = Logger.getLogger(UpdateServlet.class.getName());

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri) 
            throws IOException {
        if (uri.equals("/clear-all-data")) {
            clearAllData(resp);

        } else if (uri.equals("/upload-issues")) {
            uploadIssues(req, resp, pm);

        } else if (uri.equals("/upload-evaluation")) {
            uploadEvaluation(req, resp, pm);
        }
    }

    private void clearAllData(HttpServletResponse resp) throws IOException {
        int deleted = 0;
        try {
            DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
            PreparedQuery pquery = ds.prepare(new com.google.appengine.api.datastore.Query().setKeysOnly());
            for (Entity entity : pquery.asIterable()) {
                ds.delete(entity.getKey());
                deleted++;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not delete all data - only " + deleted + " entities", e);
        }
        setResponse(resp, 200, "Deleted " + deleted + " entities");
    }

    private void uploadIssues(HttpServletRequest req, HttpServletResponse resp,
                              PersistenceManager pm) throws IOException {
        UploadIssues issues = UploadIssues.parseFrom(req.getInputStream());
        SqlCloudSession session = lookupCloudSessionById(issues.getSessionId(), pm);
        if (session == null) {
            resp.setStatus(403);
            return;
        }
        List<String> hashes = decodeHashesForIssues(issues);
        Set<String> existingIssueHashes = lookupHashes(hashes, pm);
        for (Issue issue : issues.getNewIssuesList()) {
            if (!existingIssueHashes.contains(AppEngineProtoUtil.decodeHash(issue.getHash()))) {
                DbIssue dbIssue = createDbIssue(issue);

                commitInTransaction(pm, dbIssue);
            }
        }

        setResponse(resp, 200, "");
    }

    private void uploadEvaluation(HttpServletRequest req,
                                  HttpServletResponse resp, PersistenceManager pm) throws IOException {
        UploadEvaluation uploadEvalMsg = UploadEvaluation.parseFrom(req.getInputStream());
        SqlCloudSession session = lookupCloudSessionById(uploadEvalMsg.getSessionId(), pm);
        if (session == null) {
            setResponse(resp, 403, "not authenticated");
            return;
        }

        DbEvaluation dbEvaluation = createDbEvaluation(uploadEvalMsg.getEvaluation());
        dbEvaluation.setWho(session.getUser().getNickname());
        copyInvocationToEvaluation(pm, session, dbEvaluation);
        Transaction tx = pm.currentTransaction();
        boolean setStatusAlready = false;
        try {
            tx.begin();

            String hash = AppEngineProtoUtil.decodeHash(uploadEvalMsg.getHash());
            DbIssue issue = findIssue(pm, hash);
            if (issue == null) {
                setResponse(resp, 404, "no such issue " + AppEngineProtoUtil.decodeHash(uploadEvalMsg.getHash()));
                setStatusAlready = true;
                return;
            }
            dbEvaluation.setIssue(issue);
            issue.addEvaluation(dbEvaluation);
            pm.makePersistent(issue);

            tx.commit();

        } finally {
            if (tx.isActive()) {
                tx.rollback();
                if (!setStatusAlready) {
                    setResponse(resp, 403, "Transaction failed");
                }
            }
        }

        resp.setStatus(200);
    }

    // ========================= end of request handling ================================

    private List<String> decodeHashesForIssues(UploadIssues issues) {
        List<String> hashes = new ArrayList<String>();
        for (Issue issue : issues.getNewIssuesList()) {
            hashes.add(AppEngineProtoUtil.decodeHash(issue.getHash()));
        }
        return hashes;
    }

    private void commitInTransaction(PersistenceManager pm, DbIssue dbIssue) {
        Transaction tx = pm.currentTransaction();
        tx.begin();
        try {
            pm.makePersistent(dbIssue);
            tx.commit();
        } finally {
            if (tx.isActive()) tx.rollback();
        }
    }

    private DbIssue createDbIssue(Issue issue) {
        DbIssue dbIssue = new DbIssue();
        dbIssue.setHash(AppEngineProtoUtil.decodeHash(issue.getHash()));
        dbIssue.setBugPattern(issue.getBugPattern());
        dbIssue.setPriority(issue.getPriority());
        dbIssue.setPrimaryClass(issue.getPrimaryClass());
        dbIssue.setFirstSeen(issue.getFirstSeen());
        dbIssue.setLastSeen(issue.getFirstSeen()); // ignore last seen
        return dbIssue;
    }

    private void copyInvocationToEvaluation(PersistenceManager pm, SqlCloudSession session, DbEvaluation dbEvaluation) {
        Key invocationKey = session.getInvocation();
        if (invocationKey != null) {
            DbInvocation invocation;
            try {
                invocation = pm.getObjectById(DbInvocation.class, invocationKey);
                if (invocation != null) {
                    dbEvaluation.setInvocation(invocation.getKey());
                }
            } catch (JDOObjectNotFoundException e) {
                // ignore
            }
        }
    }

    private DbEvaluation createDbEvaluation(Evaluation protoEvaluation) {
        DbEvaluation dbEvaluation = new DbEvaluation();
        dbEvaluation.setComment(protoEvaluation.getComment());
        dbEvaluation.setDesignation(protoEvaluation.getDesignation());
        dbEvaluation.setWhen(protoEvaluation.getWhen());
        return dbEvaluation;
    }

    @SuppressWarnings("unchecked")
    private DbIssue findIssue(PersistenceManager pm, String hash) {
        Query query = pm.newQuery(DbIssue.class, "hash == :hashParam");
        Iterator<DbIssue> it = ((QueryResult) query.execute(hash)).iterator();
        if (!it.hasNext()) {
            return null;
        }
        return it.next();
    }

    @SuppressWarnings("unchecked")
    private Set<String> lookupHashes(Iterable<String> hashes, PersistenceManager pm) {
        Query query = pm.newQuery("select from " + DbIssue.class.getName()
                                  + " where :hashes.contains(hash)");
        query.setResult("hash");
        Set<String> result = new HashSet<String>((List<String>) query.execute(hashes));
        query.closeAll();
        return result;
    }
}