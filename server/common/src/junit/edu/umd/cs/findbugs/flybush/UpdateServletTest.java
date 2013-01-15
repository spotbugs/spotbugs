package edu.umd.cs.findbugs.flybush;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil.encodeHash;
import static edu.umd.cs.findbugs.flybush.UpdateServlet.ONE_DAY_IN_MILLIS;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;

import com.google.common.collect.Sets;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.SetBugLink;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.SetBugLink.Builder;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UpdateIssueTimestamps;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UpdateIssueTimestamps.IssueGroup;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil;

@SuppressWarnings({ "UnusedDeclaration" })
public abstract class UpdateServletTest extends AbstractFlybushServletTest {

    @Override
    protected AbstractFlybushServlet createServlet() {
        return new UpdateServlet();
    }

    @SuppressWarnings({ "unchecked" })
    public void testExpireSqlSessions() throws Exception {
        DbUser oldUser = persistenceHelper.createDbUser("http://some.website", "old@test.com");
        SqlCloudSession oldSession = persistenceHelper.createSqlCloudSession(100, new Date(System.currentTimeMillis() - 8
                * ONE_DAY_IN_MILLIS), oldUser.createKeyObject(), "old@test.com");
        DbUser recentUser = persistenceHelper.createDbUser("http://some.website2", "recent@test.com");
        SqlCloudSession recentSession = persistenceHelper.createSqlCloudSession(101, new Date(System.currentTimeMillis() - 6
                * ONE_DAY_IN_MILLIS), recentUser.createKeyObject(), "old@test.com");
        getPersistenceManager().makePersistentAll(oldUser, recentUser, oldSession, recentSession);

        assertEquals("old@test.com", getDbUser(findSqlSession(100).get(0).getUser()).getEmail());
        assertEquals("recent@test.com", getDbUser(findSqlSession(101).get(0).getUser()).getEmail());

        executeGet("/expire-sql-sessions");

        assertEquals(0, findSqlSession(100).size());
        assertEquals("recent@test.com", getDbUser(findSqlSession(101).get(0).getUser()).getEmail());
    }

    @SuppressWarnings({ "unchecked" })
    public void testUpdateDbJune29CommentStyle() throws Exception {
        DbIssue issue = createDbIssue("fad2");
        DbEvaluation eval = createEvaluation(issue, "someone", 200);
        persistenceHelper.convertToOldCommentStyleForTesting(eval);
        getPersistenceManager().makePersistentAll(issue);
        assertTrue(persistenceHelper.isOldCommentStyle(eval));

        executeGet("/update-db-jun29");
        getPersistenceManager().refreshAll(issue);
        assertFalse(persistenceHelper.isOldCommentStyle(eval));
    }

    @SuppressWarnings({ "unchecked" })
    public void testUpdateDbJune29PrimaryClass() throws Exception {
        DbIssue issue = createDbIssue("fad2");
        DbEvaluation eval = createEvaluation(issue, "someone", 200);
        eval.setPrimaryClass(null);
        assertNull(eval.getPrimaryClass());
        getPersistenceManager().makePersistentAll(issue);

        executeGet("/update-db-jun29");
        getPersistenceManager().refreshAll(issue);
        assertEquals("my.class", eval.getPrimaryClass());
    }

    @SuppressWarnings({ "unchecked" })
    public void testUpdateDbJune29Package() throws Exception {
        DbIssue issue = createDbIssue("fad2");
        issue.setPrimaryClass("a.b.c.D");
        DbEvaluation eval = createEvaluation(issue, "someone", 200);
        eval.setPackages(Sets.<String> newHashSet());
        getPersistenceManager().makePersistentAll(issue);

        executeGet("/update-db-jun29");
        getPersistenceManager().refreshAll(issue);
        assertTrue(eval.getPackages().contains("a"));
        assertTrue(eval.getPackages().contains("a.b"));
        assertTrue(eval.getPackages().contains("a.b.c"));
    }

    @SuppressWarnings({ "unchecked" })
    public void testUpdateEvaluationEmails() throws Exception {
        // setup
        DbUser userA = persistenceHelper.createDbUser("http://some.website", "a@a.com");
        DbEvaluation evalA = persistenceHelper.createDbEvaluation();
        evalA.setWho(userA.createKeyObject());
        evalA.setEmail("x@x.com");

        DbUser userB = persistenceHelper.createDbUser("http://some.website2", "b@b.com");
        DbEvaluation evalB = persistenceHelper.createDbEvaluation();
        evalB.setWho(userB.createKeyObject());

        PersistenceManager pm = getPersistenceManager();
        pm.makePersistentAll(userA, userB, evalA, evalB);

        // execute
        executeGet("/update-evaluation-emails");

        // verify
        pm.refreshAll(evalA, evalB);
        assertEquals("x@x.com", evalA.getEmail());
        assertEquals("b@b.com", evalB.getEmail());
    }

    public void testUpdateIssueTimestampsNotAuthenticated() throws Exception {
        UpdateIssueTimestamps tsCmd = UpdateIssueTimestamps
                .newBuilder()
                .setSessionId(555)
                .addIssueGroups(
                        IssueGroup.newBuilder().setTimestamp(SAMPLE_TIMESTAMP + 50).addIssueHashes(encodeHash("fad1")).build())
                .build();
        executePost("/update-issue-timestamps", tsCmd.toByteArray());
        checkResponse(403);
    }

    public void testUpdateIssueTimestamps() throws Exception {
        createCloudSession(555);

        PersistenceManager pm = getPersistenceManager();
        DbIssue issue1 = createDbIssue("fad1");
        DbIssue issue2 = createDbIssue("fad2");
        pm.makePersistentAll(issue1, issue2);

        UpdateIssueTimestamps tsCmd = UpdateIssueTimestamps
                .newBuilder()
                .setSessionId(555)
                .addIssueGroups(
                        IssueGroup.newBuilder().setTimestamp(SAMPLE_TIMESTAMP + 50).addIssueHashes(encodeHash("fad1"))
                                .addIssueHashes(encodeHash("fad2")).build()).build();
        executePost("/update-issue-timestamps", tsCmd.toByteArray());
        pm.refreshAll(issue1, issue2);
        checkResponse(200);
        assertEquals(SAMPLE_TIMESTAMP + 50, issue1.getFirstSeen());
        assertEquals(SAMPLE_TIMESTAMP + 50, issue2.getFirstSeen());
    }

    public void testUpdateIssueTimestampsZeroStored() throws Exception {
        createCloudSession(555);

        PersistenceManager pm = getPersistenceManager();
        DbIssue issue1 = createDbIssue("fad1");
        DbIssue issue2 = createDbIssue("fad2");
        issue2.setFirstSeen(0);
        pm.makePersistentAll(issue1, issue2);

        UpdateIssueTimestamps tsCmd = UpdateIssueTimestamps
                .newBuilder()
                .setSessionId(555)
                .addIssueGroups(
                        IssueGroup.newBuilder().setTimestamp(SAMPLE_TIMESTAMP + 50).addIssueHashes(encodeHash("fad1"))
                                .addIssueHashes(encodeHash("fad2")).build()).build();
        executePost("/update-issue-timestamps", tsCmd.toByteArray());
        pm.refreshAll(issue1, issue2);
        checkResponse(200);
        assertEquals(SAMPLE_TIMESTAMP + 50, issue1.getFirstSeen());
        assertEquals(SAMPLE_TIMESTAMP + 50, issue2.getFirstSeen());
    }

    public void testUpdateIssueTimestampsIgnoreOldTimestamp() throws Exception {
        createCloudSession(555);

        PersistenceManager pm = getPersistenceManager();
        DbIssue issue1 = createDbIssue("fad1");
        pm.makePersistentAll(issue1, issue1);

        UpdateIssueTimestamps tsCmd = UpdateIssueTimestamps.newBuilder().setSessionId(555)
                .addIssueGroups(IssueGroup.newBuilder().setTimestamp(SAMPLE_TIMESTAMP + 150).addIssueHashes(encodeHash("fad1")).build()).build();
        executePost("/update-issue-timestamps", tsCmd.toByteArray());
        pm.refreshAll(issue1);
        checkResponse(200);
        assertEquals(SAMPLE_TIMESTAMP + 100, issue1.getFirstSeen());
    }

    public void testUpdateIssueTimestampsMultipleGroups() throws Exception {
        createCloudSession(555);

        PersistenceManager pm = getPersistenceManager();
        DbIssue issue1 = createDbIssue("fad1");
        DbIssue issue2 = createDbIssue("fad2");
        pm.makePersistentAll(issue1, issue2);

        UpdateIssueTimestamps tsCmd = UpdateIssueTimestamps
                .newBuilder()
                .setSessionId(555)
                .addIssueGroups(
                        IssueGroup.newBuilder().setTimestamp(SAMPLE_TIMESTAMP + 50).addIssueHashes(encodeHash("fad1")).build())
                .addIssueGroups(
                        IssueGroup.newBuilder().setTimestamp(SAMPLE_TIMESTAMP + 25).addIssueHashes(encodeHash("fad2")).build())
                .build();
        executePost("/update-issue-timestamps", tsCmd.toByteArray());

        // verify
        pm.refreshAll(issue1, issue2);
        checkResponse(200);
        assertEquals(SAMPLE_TIMESTAMP + 50, issue1.getFirstSeen());
        assertEquals(SAMPLE_TIMESTAMP + 25, issue2.getFirstSeen());
    }

    public void testUpdateIssueTimestampsLaterThanStored() throws Exception {
        createCloudSession(555);

        PersistenceManager pm = getPersistenceManager();
        DbIssue issue1 = createDbIssue("fad1");
        DbIssue issue2 = createDbIssue("fad2");
        issue2.setFirstSeen(SAMPLE_TIMESTAMP + 25);
        pm.makePersistentAll(issue1, issue2);

        UpdateIssueTimestamps tsCmd = UpdateIssueTimestamps
                .newBuilder()
                .setSessionId(555)
                .addIssueGroups(
                        IssueGroup.newBuilder().setTimestamp(SAMPLE_TIMESTAMP + 50).addIssueHashes(encodeHash("fad1"))
                                .addIssueHashes(encodeHash("fad2")).build()).build();
        executePost("/update-issue-timestamps", tsCmd.toByteArray());

        // verify
        pm.refreshAll(issue1, issue2);
        checkResponse(200);
        assertEquals(SAMPLE_TIMESTAMP + 50, issue1.getFirstSeen());
        assertEquals(SAMPLE_TIMESTAMP + 25, issue2.getFirstSeen());
    }

    public void testUploadIssueWithoutAuthenticating() throws Exception {
        Issue issue = createProtoIssue("fad");
        UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(issue).build();
        executePost("/upload-issues", issuesToUpload.toByteArray());

        // verify
        checkResponse(403);
    }

    @SuppressWarnings("unchecked")
    public void testUploadIssue() throws Exception {
        // setup
        createCloudSession(555);
        Issue issue = createProtoIssue("fad");

        // execute
        UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(issue).build();
        executePost("/upload-issues", issuesToUpload.toByteArray());
        checkResponse(200, "");

        // verify
        List<DbIssue> dbIssues = getAllIssuesFromDb();
        assertEquals(1, dbIssues.size());

        DbIssue dbIssue = dbIssues.get(0);
        checkIssuesEqualExceptTimestamps(dbIssue, issue);
        assertEquals(issue.getFirstSeen(), dbIssue.getFirstSeen());
        assertEquals(issue.getFirstSeen(), dbIssue.getLastSeen()); // upon
                                                                   // initial
                                                                   // upload,
                                                                   // should be
                                                                   // identical
    }

    public void testUploadIssueWithBadToken() throws Exception {
        Issue issue = createProtoIssue("fad");
        UploadIssues issuesToUpload = UploadIssues.newBuilder().setToken("xxx").addNewIssues(issue).build();
        executePost("/upload-issues", issuesToUpload.toByteArray());

        // verify
        checkResponse(403);
    }

    public void testUploadIssueWithGoodToken() throws Exception {
        DbUser user = persistenceHelper.createDbUser("http://example.com", "xyz");
        user.setUploadToken("abc");
        persistenceHelper.getPersistenceManager().makePersistent(user);

        // setup
        Issue issue = createProtoIssue("fad");

        // execute
        UploadIssues issuesToUpload = UploadIssues.newBuilder().setToken("abc").addNewIssues(issue).build();
        executePost("/upload-issues", issuesToUpload.toByteArray());
        checkResponse(200, "");

        // verify
        List<DbIssue> dbIssues = getAllIssuesFromDb();
        assertEquals(1, dbIssues.size());

        DbIssue dbIssue = dbIssues.get(0);
        checkIssuesEqualExceptTimestamps(dbIssue, issue);
        assertEquals(issue.getFirstSeen(), dbIssue.getFirstSeen());
        assertEquals(issue.getFirstSeen(), dbIssue.getLastSeen()); // upon
                                                                   // initial
                                                                   // upload,
                                                                   // should be
                                                                   // identical
    }

    @SuppressWarnings("unchecked")
    public void testUploadMultipleIssues() throws Exception {
        createCloudSession(555);
        Issue issue1 = createProtoIssue("fad1");
        Issue issue2 = createProtoIssue("fad2");
        UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(issue1).addNewIssues(issue2)
                .build();
        executePost("/upload-issues", issuesToUpload.toByteArray());

        // verify
        checkResponse(200, "");
        List<DbIssue> dbIssues = (List<DbIssue>) getPersistenceManager().newQuery(
                "select from " + persistenceHelper.getDbIssueClassname()).execute();
        assertEquals(2, dbIssues.size());

        checkIssuesEqualExceptTimestamps(dbIssues.get(0), issue1);
        checkIssuesEqualExceptTimestamps(dbIssues.get(1), issue2);
    }

    @SuppressWarnings("unchecked")
    public void testUploadIssuesWhichAlreadyExist() throws Exception {
        createCloudSession(555);
        DbIssue oldDbIssue = createDbIssue("fad1");
        getPersistenceManager().makePersistent(oldDbIssue);
        Issue oldIssue = createProtoIssue("fad1");
        Issue newIssue = createProtoIssue("fad2");
        UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(oldIssue).addNewIssues(newIssue)
                .build();
        executePost("/upload-issues", issuesToUpload.toByteArray());

        // verify
        checkResponse(200, "");
        List<DbIssue> dbIssues = (List<DbIssue>) getPersistenceManager().newQuery(
                "select from " + persistenceHelper.getDbIssueClassname() + " order by hash ascending").execute();
        assertEquals(2, dbIssues.size());

        assertEquals("fad1", dbIssues.get(0).getHash());
        assertEquals("fad2", dbIssues.get(1).getHash());
    }

    public void testUploadEvaluationNoAuth() throws Exception {
        executePost("/upload-evaluation", UploadEvaluation.newBuilder().setSessionId(555).setHash(encodeHash("fad"))
                .setEvaluation(createProtoEvaluation()).build().toByteArray());
        // verify
        checkResponse(403, "not authenticated");
    }

    public void testUploadEvaluationWithoutFindIssuesFirst() throws Exception {
        createCloudSession(555);

        DbIssue dbIssue = createDbIssue("fad");
        dbIssue.setPrimaryClass("a.b.c.D");
        getPersistenceManager().makePersistent(dbIssue);
        Evaluation protoEval = createProtoEvaluation();
        executePost("/upload-evaluation", UploadEvaluation.newBuilder().setSessionId(555).setHash(encodeHash("fad"))
                .setEvaluation(protoEval).build().toByteArray());

        // verify
        checkResponse(200);
        getPersistenceManager().refresh(dbIssue);
        assertEquals(1, dbIssue.getEvaluations().size());
        DbEvaluation dbEval = dbIssue.getEvaluations().iterator().next();
        assertEquals(protoEval.getComment(), dbEval.getComment());
        assertFalse("should be new comment style", persistenceHelper.isOldCommentStyle(dbEval));
        assertEquals(protoEval.getDesignation(), dbEval.getDesignation());
        assertEquals(protoEval.getWhen(), dbEval.getWhen());
        assertEquals("my@email.com", getDbUser(dbEval.getWho()).getEmail());
        assertEquals("a.b.c.D", dbEval.getPrimaryClass());
        assertTrue(dbEval.getPackages().contains("a"));
        assertTrue(dbEval.getPackages().contains("a.b"));
        assertTrue(dbEval.getPackages().contains("a.b.c"));
        assertFalse(dbEval.getPackages().contains("a.b.c.D"));
        assertNull(dbEval.getInvocationKey());
    }

    public void testUploadEvaluationMoreThan500chars() throws Exception {
        createCloudSession(555);

        DbIssue dbIssue = createDbIssue("fad");
        getPersistenceManager().makePersistent(dbIssue);
        char[] array = new char[600];
        Arrays.fill(array, 'x');
        Evaluation protoEval = Evaluation.newBuilder().setDesignation("MUST_FIX").setComment(new String(array)).setWhen(100)
                .build();
        executePost("/upload-evaluation", UploadEvaluation.newBuilder().setSessionId(555).setHash(encodeHash("fad"))
                .setEvaluation(protoEval).build().toByteArray());
        checkResponse(200);
        getPersistenceManager().refresh(dbIssue);
        assertEquals(1, dbIssue.getEvaluations().size());
        DbEvaluation dbEval = dbIssue.getEvaluations().iterator().next();
        assertEquals(protoEval.getComment(), dbEval.getComment());
        assertEquals(protoEval.getDesignation(), dbEval.getDesignation());
        assertEquals(protoEval.getWhen(), dbEval.getWhen());
        assertEquals("my@email.com", getDbUser(dbEval.getWho()).getEmail());
        assertEquals("my.class", dbEval.getPrimaryClass());
        assertNull(dbEval.getInvocationKey());
    }

    public void testUploadEvaluationWithFindIssuesFirst() throws Exception {
        // setup
        createCloudSession(555);

        // execute log-in
        executePost(authServlet, "/log-in", LogIn.newBuilder().setSessionId(555).setAnalysisTimestamp(100).build().toByteArray());
        checkResponse(200);

        // execute find-issues
        initServletAndMocks();
        QueryServlet queryServlet = new QueryServlet();
        queryServlet.setPersistenceHelper(testHelper.createPersistenceHelper(getPersistenceManager()));
        initServletAndMocks();
        executePost(queryServlet, "/find-issues", FindIssues.newBuilder().setSessionId(555).addMyIssueHashes(encodeHash("abc"))
                .build().toByteArray());
        checkResponse(200);

        // execute upload-evaluation
        initServletAndMocks();
        DbIssue dbIssue = createDbIssue("fad");
        getPersistenceManager().makePersistent(dbIssue);
        Evaluation protoEval = createProtoEvaluation();
        executePost("/upload-evaluation", UploadEvaluation.newBuilder().setSessionId(555).setHash(encodeHash("fad"))
                .setEvaluation(protoEval).build().toByteArray());

        // verify
        checkResponse(200);

        // evaluations
        assertEquals(1, dbIssue.getEvaluations().size());
        DbEvaluation dbEval = dbIssue.getEvaluations().iterator().next();
        assertEquals(protoEval.getComment(), dbEval.getComment());
        assertEquals(protoEval.getDesignation(), dbEval.getDesignation());
        assertEquals(protoEval.getWhen(), dbEval.getWhen());
        assertEquals("my@email.com", getDbUser(dbEval.getWho()).getEmail());
        assertEquals("my@email.com", dbEval.getEmail());

        // invocation
        Object invocationId = dbEval.getInvocationKey();
        assertNotNull(invocationId);
        DbInvocation invocation = persistenceHelper.getObjectById(getPersistenceManager(),
                persistenceHelper.getDbInvocationClass(), invocationId);
        assertEquals("my@email.com", getDbUser(invocation.getWho()).getEmail());
        assertEquals(100, invocation.getStartTime());
    }

    public void testUploadEvaluationNonexistentIssue() throws Exception {
        createCloudSession(555);

        Evaluation protoEval = createProtoEvaluation();
        executePost("/upload-evaluation", UploadEvaluation.newBuilder().setSessionId(555).setHash(encodeHash("faf"))
                .setEvaluation(protoEval).build().toByteArray());
        checkResponse(404, "no such issue faf\n");
    }

    public void testSetBugLinkNotAuthenticated() throws Exception {
        setBugLinkExpectResponse(403, "fad", "GOOGLE_CODE", "http://my.bug/123");
    }

    public void testSetBugLinkNonexistentBug() throws Exception {
        createCloudSession(555);
        setBugLinkExpectResponse(404, "fad", "GOOGLE_CODE", "http://my.bug/123");
    }

    public void testSetBugLinkGoogleCode() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "GOOGLE_CODE", "http://my.bug/123");
        checkBugLinkInDb("GOOGLE_CODE", "http://my.bug/123");
    }

    public void testSetBugLinkJira() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "JIRA", "http://my.bug/123");
        checkBugLinkInDb("JIRA", "http://my.bug/123");
    }

    public void testSetBugLinkNullType() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", null, "http://my.bug/123");
        checkBugLinkInDb(null, "http://my.bug/123");
    }

    public void testSetBugLinkTrimsSpace() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "GOOGLE_CODE", "  http://my.bug/123   ");
        checkBugLinkInDb("GOOGLE_CODE", "http://my.bug/123");
    }

    public void testUpdateExistingBugLink() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "GOOGLE_CODE", "http://my.bug/123");
        checkBugLinkInDb("GOOGLE_CODE", "http://my.bug/123");
        setBugLink("fad", "GOOGLE_CODE", "http://my.bug/456");
        checkBugLinkInDb("GOOGLE_CODE", "http://my.bug/456");
    }

    public void testClearBugLink() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "GOOGLE_CODE", "http://my.bug/123");
        checkBugLinkInDb("GOOGLE_CODE", "http://my.bug/123");
        setBugLink("fad", "GOOGLE_CODE", null);
        checkBugLinkInDb("GOOGLE_CODE", null);
    }

    public void testClearBugLinkWithEmptyString() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "GOOGLE_CODE", "http://my.bug/123");
        checkBugLinkInDb("GOOGLE_CODE", "http://my.bug/123");
        setBugLink("fad", "GOOGLE_CODE", "");
        checkBugLinkInDb("GOOGLE_CODE", null);
    }

    public void testClearBugLinkWithSpace() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "GOOGLE_CODE", "http://my.bug/123");
        checkBugLinkInDb("GOOGLE_CODE", "http://my.bug/123");
        setBugLink("fad", "GOOGLE_CODE", "  ");
        checkBugLinkInDb("GOOGLE_CODE", null);
    }

    // TODO: I suspect this doesn't work due to DatastoreService and
    // PersistenceManager sync issues
    @SuppressWarnings({ "unchecked", "ConstantConditions", "UnusedDeclaration" })
    public void BROKEN_testClearAllData() throws Exception {
        createCloudSession(555);

        DbIssue foundIssue = createDbIssue("fad1");
        DbEvaluation eval1 = createEvaluation(foundIssue, "first", 100);
        DbEvaluation eval2 = createEvaluation(foundIssue, "second", 200);
        DbEvaluation eval3 = createEvaluation(foundIssue, "first", 300);
        foundIssue.addEvaluation(eval1);
        foundIssue.addEvaluation(eval2);
        foundIssue.addEvaluation(eval3);

        // apparently the evaluation is automatically persisted. throws
        // exception when attempting to persist the eval with the issue.
        getPersistenceManager().makePersistent(foundIssue);

        executePost("/clear-all-data", new byte[0]);
        checkResponse(200);

        getPersistenceManager().close();
        testHelper.initPersistenceManager();

        for (Class<?> cls : Arrays.asList(persistenceHelper.getDbIssueClass(), persistenceHelper.getDbIssueClass(),
                persistenceHelper.getDbInvocationClass(), persistenceHelper.getSqlCloudSessionClass())) {
            try {
                List<?> objs = (List<?>) getPersistenceManager().newQuery("select from " + cls.getName()).execute();
                fail("some entities still exist: " + cls.getSimpleName() + ": " + objs);
            } catch (Exception ignored) {
            }
        }
    }

    // ========================= end of tests ================================

    @SuppressWarnings({ "unchecked" })
    private List<SqlCloudSession> findSqlSession(long sessionId) {
        return (List<SqlCloudSession>) getPersistenceManager().newQuery(
                "select from " + persistenceHelper.getSqlCloudSessionClassname() + " where randomID == :val").execute(
                Long.toString(sessionId));
    }

    private void checkBugLinkInDb(String expectedType, String expectedUrl) {
        List<DbIssue> dbIssues = getAllIssuesFromDb();
        assertEquals(1, dbIssues.size());
        DbIssue issue = dbIssues.get(0);
        String dbBugLink = issue.getBugLink();
        if (expectedUrl == null)
            assertNull(dbBugLink);
        else
            assertEquals(expectedUrl, dbBugLink);

        String dbBugLinkType = issue.getBugLinkType();
        if (expectedType == null)
            assertNull("" + dbBugLinkType, dbBugLinkType);
        else
            assertEquals(expectedType, dbBugLinkType);
    }

    @SuppressWarnings("unchecked")
    private List<DbIssue> getAllIssuesFromDb() {
        Query query = getPersistenceManager().newQuery("select from " + persistenceHelper.getDbIssueClassname());
        return (List<DbIssue>) query.execute();
    }

    private void setBugLink(String hash, String linkType, String link) throws IOException, ServletException {
        setBugLinkExpectResponse(200, hash, linkType, link);
    }

    private void setBugLinkExpectResponse(int responseCode, String hash, String linkType, String link) throws IOException,
            ServletException {
        initServletAndMocks();
        Builder setBugLink = SetBugLink.newBuilder().setSessionId(555).setHash(encodeHash(hash));
        if (link != null)
            setBugLink.setUrl(link);
        if (linkType != null) {
            setBugLink.setBugLinkType(linkType);
        }

        executePost("/set-bug-link", setBugLink.build().toByteArray());
        checkResponse(responseCode);
    }

    private void uploadIssue(String hash) throws IOException {
        Issue issue = createProtoIssue(hash);

        // create issue
        UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(issue).build();
        executePost("/upload-issues", issuesToUpload.toByteArray());
        checkResponse(200);

        // verify initial upload
        List<DbIssue> dbIssues = getAllIssuesFromDb();
        assertEquals(1, dbIssues.size());
        assertNull(dbIssues.get(0).getBugLink());
    }

    private Evaluation createProtoEvaluation() {
        return Evaluation.newBuilder().setDesignation("MUST_FIX").setComment("my comment").setWhen(100).build();
    }

    private Issue createProtoIssue(String patternAndHash) {
        patternAndHash = WebCloudProtoUtil.normalizeHash(patternAndHash);
        Issue.Builder issueBuilder = Issue.newBuilder();
        issueBuilder.setHash(encodeHash(patternAndHash));
        issueBuilder.setBugPattern(patternAndHash);
        issueBuilder.setPriority(2);
        issueBuilder.setPrimaryClass("my.class");
        issueBuilder.setFirstSeen(100);
        issueBuilder.setLastSeen(200);

        return issueBuilder.build();
    }
}
