package edu.umd.cs.findbugs.flybush;

import com.google.appengine.api.datastore.Key;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.SetBugLink;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.SetBugLink.Builder;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;

import javax.jdo.Query;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.encodeHash;

public class UpdateServletTest extends AbstractFlybushServletTest {
    @Override
    protected AbstractFlybushServlet createServlet() {
        return new UpdateServlet();
    }

	public void testUploadIssueWithoutAuthenticating() throws Exception {
		Issue issue = createProtoIssue("fad");
		UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(issue).build();
		executePost("/upload-issues", issuesToUpload.toByteArray());
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
        FlybushServletUtil.checkIssuesEqualExceptTimestamps(dbIssue, issue);
        assertEquals(issue.getFirstSeen(), dbIssue.getFirstSeen());
        assertEquals(issue.getFirstSeen(), dbIssue.getLastSeen()); // upon initial upload, should be identical
	}

	@SuppressWarnings("unchecked")
	public void testUploadMultipleIssues() throws Exception {
    	createCloudSession(555);
		Issue issue1 = createProtoIssue("fad1");
		Issue issue2 = createProtoIssue("fad2");
		UploadIssues issuesToUpload = UploadIssues.newBuilder()
				.setSessionId(555).addNewIssues(issue1).addNewIssues(issue2)
				.build();
		executePost("/upload-issues", issuesToUpload.toByteArray());
		checkResponse(200, "");
		List<DbIssue> dbIssues = (List<DbIssue>) persistenceManager
				.newQuery("select from " + DbIssue.class.getName()).execute();
		assertEquals(2, dbIssues.size());

        FlybushServletUtil.checkIssuesEqualExceptTimestamps(dbIssues.get(0), issue1);
        FlybushServletUtil.checkIssuesEqualExceptTimestamps(dbIssues.get(1), issue2);
    }

	@SuppressWarnings("unchecked")
	public void testUploadIssuesWhichAlreadyExist() throws Exception {
    	createCloudSession(555);
        DbIssue oldDbIssue = FlybushServletUtil.createDbIssue("fad1");
		persistenceManager.makePersistent(oldDbIssue);
		Issue oldIssue = createProtoIssue("fad1");
		Issue newIssue = createProtoIssue("fad2");
		UploadIssues issuesToUpload = UploadIssues.newBuilder()
				.setSessionId(555)
				.addNewIssues(oldIssue)
				.addNewIssues(newIssue)
				.build();
		executePost("/upload-issues", issuesToUpload.toByteArray());
		checkResponse(200, "");
		List<DbIssue> dbIssues = (List<DbIssue>) persistenceManager
				.newQuery("select from " + DbIssue.class.getName() + " order by hash").execute();
		assertEquals(2, dbIssues.size());

		assertEquals("fad1", dbIssues.get(0).getHash());
		assertEquals("fad2", dbIssues.get(1).getHash());
	}

	public void testUploadEvaluationNoAuth() throws Exception {
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash(encodeHash("fad"))
				.setEvaluation(createProtoEvaluation())
				.build().toByteArray());
		checkResponse(403, "not authenticated");
	}

	public void testUploadEvaluationWithoutFindIssuesFirst() throws Exception {
		createCloudSession(555);

        DbIssue dbIssue = FlybushServletUtil.createDbIssue("fad");
		persistenceManager.makePersistent(dbIssue);
		Evaluation protoEval = createProtoEvaluation();
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash(encodeHash("fad"))
				.setEvaluation(protoEval)
				.build().toByteArray());
		checkResponse(200);
		persistenceManager.refresh(dbIssue);
		assertEquals(1, dbIssue.getEvaluations().size());
		DbEvaluation dbEval = dbIssue.getEvaluations().iterator().next();
		assertEquals(protoEval.getComment(), dbEval.getComment());
		assertEquals(protoEval.getDesignation(), dbEval.getDesignation());
		assertEquals(protoEval.getWhen(), dbEval.getWhen());
		assertEquals("my@email.com", dbEval.getWho());
		assertNull(dbEval.getInvocation());
	}

	public void testUploadEvaluationWithFindIssuesFirst() throws Exception {
		createCloudSession(555);

		executePost(authServlet, "/log-in", LogIn.newBuilder()
				.setSessionId(555)
                .setAnalysisTimestamp(100)
				.build().toByteArray());
		checkResponse(200);
		initServletAndMocks();

        QueryServlet queryServlet = new QueryServlet();
        queryServlet.setPersistenceManager(persistenceManager);
        initServletAndMocks();
        executePost(queryServlet, "/find-issues", FindIssues.newBuilder()
				.setSessionId(555)
				.addMyIssueHashes(encodeHash("abc"))
				.build().toByteArray());
		checkResponse(200);
		initServletAndMocks();

        DbIssue dbIssue = FlybushServletUtil.createDbIssue("fad");
		persistenceManager.makePersistent(dbIssue);
		Evaluation protoEval = createProtoEvaluation();
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash(encodeHash("fad"))
				.setEvaluation(protoEval)
				.build().toByteArray());
		checkResponse(200);
		assertEquals(1, dbIssue.getEvaluations().size());
		DbEvaluation dbEval = dbIssue.getEvaluations().iterator().next();
		assertEquals(protoEval.getComment(), dbEval.getComment());
		assertEquals(protoEval.getDesignation(), dbEval.getDesignation());
		assertEquals(protoEval.getWhen(), dbEval.getWhen());
		assertEquals("my@email.com", dbEval.getWho());
		Key invocationId = dbEval.getInvocation();
		assertNotNull(invocationId);
		DbInvocation invocation = persistenceManager.getObjectById(DbInvocation.class,
                                                                   invocationId);
		assertEquals("my@email.com", invocation.getWho());
		assertEquals(100, invocation.getStartTime());
	}

	public void testUploadEvaluationNonexistentIssue() throws Exception {
		createCloudSession(555);

		Evaluation protoEval = createProtoEvaluation();
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash(encodeHash("faf"))
				.setEvaluation(protoEval)
				.build().toByteArray());
		checkResponse(404, "no such issue faf\n");
	}

    public void testSetBugLinkNotAuthenticated() throws Exception {
        setBugLinkExpectResponse(403, "fad", "http://my.bug/123");
    }

    public void testSetBugLinkNonexistentBug() throws Exception {
        createCloudSession(555);
        setBugLinkExpectResponse(404, "fad", "http://my.bug/123");
    }

    public void testSetBugLink() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "http://my.bug/123");
        checkBugLinkInDb("http://my.bug/123");
    }

    public void testSetBugLinkTrimsSpace() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "  http://my.bug/123   ");
        checkBugLinkInDb("http://my.bug/123");
    }

    public void testUpdateExistingBugLink() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "http://my.bug/123");
        checkBugLinkInDb("http://my.bug/123");
        setBugLink("fad", "http://my.bug/456");
        checkBugLinkInDb("http://my.bug/456");
    }

    public void testClearBugLink() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "http://my.bug/123");
        checkBugLinkInDb("http://my.bug/123");
        setBugLink("fad", null);
        checkBugLinkInDb(null);
    }

    public void testClearBugLinkWithEmptyString() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "http://my.bug/123");
        checkBugLinkInDb("http://my.bug/123");
        setBugLink("fad", "");
        checkBugLinkInDb(null);
    }

    public void testClearBugLinkWithSpace() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", "http://my.bug/123");
        checkBugLinkInDb("http://my.bug/123");
        setBugLink("fad", "  ");
        checkBugLinkInDb(null);
    }

    // TODO: I suspect this doesn't work due to DatastoreService and PersistenceManager sync issues
    @SuppressWarnings({"unchecked", "ConstantConditions", "UnusedDeclaration"})
    public void BROKEN_testClearAllData() throws Exception {
    	createCloudSession(555);

        DbIssue foundIssue = FlybushServletUtil.createDbIssue("fad1");
        DbEvaluation eval1 = FlybushServletUtil.createEvaluation(foundIssue, "first", 100);
        DbEvaluation eval2 = FlybushServletUtil.createEvaluation(foundIssue, "second", 200);
        DbEvaluation eval3 = FlybushServletUtil.createEvaluation(foundIssue, "first", 300);
		foundIssue.addEvaluation(eval1);
		foundIssue.addEvaluation(eval2);
		foundIssue.addEvaluation(eval3);

		// apparently the evaluation is automatically persisted. throws
		// exception when attempting to persist the eval with the issue.
		persistenceManager.makePersistent(foundIssue);

    	executePost("/clear-all-data", new byte[0]);
		checkResponse(200);


        persistenceManager.close();
        initPersistenceManager();

        for (Class<?> cls : Arrays.asList(DbIssue.class, DbEvaluation.class, DbInvocation.class, SqlCloudSession.class)) {
            try {
                List objs = (List) persistenceManager.newQuery("select from " + cls.getName()).execute();
                fail("some entities still exist: " + cls.getSimpleName() + ": " + objs);
            } catch (Exception e) {
            }
        }
	}

	// ========================= end of tests ================================

    private void checkBugLinkInDb(String bugLink) {
        List<DbIssue> dbIssues = getAllIssuesFromDb();
        assertEquals(1, dbIssues.size());
        String dbBugLink = dbIssues.get(0).getBugLink();
        if (bugLink == null)
            assertNull(dbBugLink);
        else
            assertEquals(bugLink, dbBugLink);
    }

    @SuppressWarnings("unchecked")
    private List<DbIssue> getAllIssuesFromDb() {
        Query query = persistenceManager.newQuery("select from " + DbIssue.class.getName());
        return (List<DbIssue>) query.execute();
    }

    private void setBugLink(String hash, String link) throws IOException {
        setBugLinkExpectResponse(200, hash, link);
    }

    private void setBugLinkExpectResponse(int responseCode, String hash, String link) throws IOException {
        initServletAndMocks();
        Builder setBugLink = SetBugLink.newBuilder()
                .setSessionId(555)
                .setHash(encodeHash(hash));
        if (link != null)
            setBugLink.setUrl(link);

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
        return Evaluation.newBuilder()
                .setDesignation("MUST_FIX")
                .setComment("my comment")
                .setWhen(100)
                .build();
	}

    private Issue createProtoIssue(String patternAndHash) {
		patternAndHash = AppEngineProtoUtil.normalizeHash(patternAndHash);
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