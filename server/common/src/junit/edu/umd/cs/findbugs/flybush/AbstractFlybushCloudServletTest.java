package edu.umd.cs.findbugs.flybush;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil.decodeHash;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.Query;
import javax.servlet.ServletException;

import org.junit.Assert;
import org.mockito.Mockito;

import com.dyuproject.openid.OpenIdUser;
import com.dyuproject.openid.ext.AxSchemaExtension;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil;

public abstract class AbstractFlybushCloudServletTest extends AbstractFlybushServletTest<PersistenceHelper> {

   
    protected AuthServlet authServlet;


    @Override
    protected void initServletAndMocks() throws IOException, ServletException {
        authServlet = new AuthServlet();
        authServlet.setPersistenceHelper(persistenceHelper);

        super.initServletAndMocks();
    }

    protected void initOpenidUserParameter() {
        OpenIdUser user = new OpenIdUser();
        HashMap<String, String> jsonAttr = new HashMap<String, String>();
        jsonAttr.put("b", "http://some.website");
        user.fromJSON(jsonAttr);
        Map<String, String> axattr = new HashMap<String, String>();
        user.setAttribute(AxSchemaExtension.ATTR_NAME, axattr);
        axattr.put("email", "my@email.com");
        Mockito.when(mockRequest.getAttribute(OpenIdUser.ATTR_NAME)).thenReturn(user);
    }

    protected void createCloudSession(long sessionId) throws IOException, ServletException {
        initOpenidUserParameter();
        executeGet(authServlet, "/browser-auth/" + sessionId);
        initServletAndMocks();
    }
    protected DbEvaluation createEvaluation(DbIssue issue, String who, long when) {
        return createEvaluation(issue, who, when, "MUST_FIX", "my comment");
    }

    @SuppressWarnings({ "unchecked" })
    protected DbEvaluation createEvaluation(DbIssue issue, String who, long when, String designation, String comment) {
        DbUser user;
        Query query = getPersistenceManager().newQuery(
                "select from " + persistenceHelper.getDbUserClassname() + " where openid == :myopenid");
        List<DbUser> results = (List<DbUser>) query.execute("http://" + who);
        if (results.isEmpty()) {
            user = persistenceHelper.createDbUser("http://" + who, who);
            getPersistenceManager().makePersistent(user);

        } else {
            user = results.iterator().next();
        }
        query.closeAll();
        DbEvaluation eval = persistenceHelper.createDbEvaluation();
        eval.setComment(comment);
        eval.setDesignation(designation);
        eval.setIssue(issue);
        eval.setWhen(SAMPLE_TIMESTAMP + when);
        eval.setWho(user.createKeyObject());
        eval.setPrimaryClass(issue.getPrimaryClass());
        eval.setPackages(UpdateServlet.buildPackageList(issue.getPrimaryClass()));
        eval.setEmail(who);
        issue.addEvaluation(eval);
        return eval;
    }
    
    protected DbUser getDbUser(Object user) {
        Assert.assertNotNull(user);
        return persistenceHelper.getObjectById(getPersistenceManager(), persistenceHelper.getDbUserClass(), user);
    }

    protected DbIssue createDbIssue(String patternAndHash) {
        patternAndHash = WebCloudProtoUtil.normalizeHash(patternAndHash);
        DbIssue foundIssue = persistenceHelper.createDbIssue();
        foundIssue.setHash(patternAndHash);
        foundIssue.setBugPattern(patternAndHash);
        foundIssue.setPriority(2);
        foundIssue.setPrimaryClass("my.class");
        foundIssue.setFirstSeen(SAMPLE_TIMESTAMP + 100);
        foundIssue.setLastSeen(SAMPLE_TIMESTAMP + 200);
        foundIssue.setBugLinkType("JIRA");
        foundIssue.setBugLink("http://bug.link");
        return foundIssue;
    }

    protected static void checkIssuesEqualExceptTimestamps(DbIssue dbIssue, Issue protoIssue) {
        assertEquals(dbIssue.getHash(), decodeHash(protoIssue.getHash()));
        assertEquals(dbIssue.getBugPattern(), protoIssue.getBugPattern());
        assertEquals(dbIssue.getPriority(), protoIssue.getPriority());
        assertEquals(dbIssue.getPrimaryClass(), protoIssue.getPrimaryClass());
        assertEquals(dbIssue.getBugLink(), protoIssue.hasBugLink() ? protoIssue.getBugLink() : null);
    }

}
