package edu.umd.cs.findbugs.flybush;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssuesResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetRecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;

import java.util.Arrays;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.decodeHash;
import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.encodeHash;
import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.encodeHashes;

public class QueryServletTest extends AbstractFlybushServletTest {

    @Override
    protected AbstractFlybushServlet createServlet() {
        return new QueryServlet();
    }

	public void testFindIssuesOneFoundNoEvaluations() throws Exception {
    	createCloudSession(555);

		DbIssue foundIssue = FlybushServletUtil.createDbIssue("FAD1");
		persistenceManager.makePersistent(foundIssue);

		FindIssues findIssuesMsg = createAuthenticatedFindIssues("FAD1", "FAD2").build();
		executePost("/find-issues", findIssuesMsg.toByteArray());
		FindIssuesResponse result = FindIssuesResponse.parseFrom(outputCollector.toByteArray());
		assertEquals(2, result.getFoundIssuesCount());

        Issue protoIssue0 = result.getFoundIssues(0);
        assertEquals(100, protoIssue0.getFirstSeen());
        assertEquals(200, protoIssue0.getLastSeen());
        assertEquals(0, protoIssue0.getEvaluationsCount());
        assertFalse(protoIssue0.hasBugPattern());
        assertFalse(protoIssue0.hasHash());
        assertFalse(protoIssue0.hasPriority());
        assertFalse(protoIssue0.hasPrimaryClass());

        Issue protoIssue1 = result.getFoundIssues(1);
        checkAllFieldsAreBlank(protoIssue1);
    }

    public void testFindIssuesWithEvaluations() throws Exception {
    	createCloudSession(555);

		DbIssue foundIssue = FlybushServletUtil.createDbIssue("fad2");
		DbEvaluation eval = FlybushServletUtil.createEvaluation(foundIssue, "someone", 100);
		foundIssue.addEvaluation(eval);

		// apparently the evaluation is automatically persisted. throws
		// exception when attempting to persist the eval with the issue.
		persistenceManager.makePersistent(foundIssue);

		FindIssues findIssues = createAuthenticatedFindIssues("fad1", "fad2").build();
		executePost("/find-issues", findIssues.toByteArray());
		FindIssuesResponse result = FindIssuesResponse.parseFrom(outputCollector.toByteArray());
		assertEquals(2, result.getFoundIssuesCount());

		// check issues
        checkAllFieldsAreBlank(result.getFoundIssues(0));

		Issue foundissueProto = result.getFoundIssues(1);
        assertEquals(100, foundissueProto.getFirstSeen());
        assertEquals(200, foundissueProto.getLastSeen());

		// check evaluations
		assertEquals(1, foundissueProto.getEvaluationsCount());
		checkEvaluationsEqual(eval, foundissueProto.getEvaluations(0));
	}

	public void testFindIssuesOnlyShowsLatestEvaluationFromEachPerson() throws Exception {
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

		FindIssues hashesToFind = createAuthenticatedFindIssues("fad2", "fad1").build();
		executePost("/find-issues", hashesToFind.toByteArray());
		FindIssuesResponse result = FindIssuesResponse.parseFrom(outputCollector.toByteArray());
		assertEquals(2, result.getFoundIssuesCount());

		// check issues
        checkAllFieldsAreBlank(result.getFoundIssues(0));

		Issue foundIssueProto = result.getFoundIssues(1);
		assertEquals(100, foundIssueProto.getFirstSeen());
		assertEquals(200, foundIssueProto.getLastSeen());

		// check evaluations
		assertEquals(2, foundIssueProto.getEvaluationsCount());
		checkEvaluationsEqual(eval2, foundIssueProto.getEvaluations(0));
		checkEvaluationsEqual(eval3, foundIssueProto.getEvaluations(1));
	}

	public void testGetRecentEvaluationsNoAuth() throws Exception {
		executePost("/get-recent-evaluations", createRecentEvalsRequest(100).toByteArray());
		checkResponse(403, "not authenticated");
	}

	public void testGetRecentEvaluations() throws Exception {
		createCloudSession(555);

		DbIssue issue = FlybushServletUtil.createDbIssue("fad");
		DbEvaluation eval1 = FlybushServletUtil.createEvaluation(issue, "someone1", 100);
		DbEvaluation eval2 = FlybushServletUtil.createEvaluation(issue, "someone2", 200);
		DbEvaluation eval3 = FlybushServletUtil.createEvaluation(issue, "someone3", 300);
		issue.addEvaluations(eval1, eval2, eval3);

		persistenceManager.makePersistent(issue);

		executePost("/get-recent-evaluations", createRecentEvalsRequest(150).toByteArray());
		checkResponse(200);
		RecentEvaluations result = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(1, result.getIssuesCount());

		// check issues
		Issue foundissueProto = result.getIssues(0);
		FlybushServletUtil.checkIssuesEqualExceptTimestamps(issue, foundissueProto);

		// check evaluations
		assertEquals(2, foundissueProto.getEvaluationsCount());
		checkEvaluationsEqual(eval2, foundissueProto.getEvaluations(0));
		checkEvaluationsEqual(eval3, foundissueProto.getEvaluations(1));
	}

	public void testGetRecentEvaluationsOnlyShowsLatestFromEachPerson() throws Exception {
		createCloudSession(555);

		DbIssue issue = FlybushServletUtil.createDbIssue("fad");
		DbEvaluation eval1 = FlybushServletUtil.createEvaluation(issue, "first",  100);
		DbEvaluation eval2 = FlybushServletUtil.createEvaluation(issue, "second", 200);
		DbEvaluation eval3 = FlybushServletUtil.createEvaluation(issue, "first",  300);
		DbEvaluation eval4 = FlybushServletUtil.createEvaluation(issue, "second", 400);
		DbEvaluation eval5 = FlybushServletUtil.createEvaluation(issue, "first",  500);
		issue.addEvaluations(eval1, eval2, eval3, eval4, eval5);

		persistenceManager.makePersistent(issue);

		executePost("/get-recent-evaluations", createRecentEvalsRequest(150).toByteArray());
		checkResponse(200);
		RecentEvaluations result = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(1, result.getIssuesCount());

		// check issues
		Issue foundissueProto = result.getIssues(0);
		FlybushServletUtil.checkIssuesEqualExceptTimestamps(issue, foundissueProto);

		// check evaluations
		assertEquals(2, foundissueProto.getEvaluationsCount());
		checkEvaluationsEqual(eval4, foundissueProto.getEvaluations(0));
		checkEvaluationsEqual(eval5, foundissueProto.getEvaluations(1));
	}

	public void testGetRecentEvaluationsNoneFound() throws Exception {
		createCloudSession(555);

		DbIssue issue = FlybushServletUtil.createDbIssue("fad");
		DbEvaluation eval1 = FlybushServletUtil.createEvaluation(issue, "someone", 100);
		DbEvaluation eval2 = FlybushServletUtil.createEvaluation(issue, "someone", 200);
		DbEvaluation eval3 = FlybushServletUtil.createEvaluation(issue, "someone", 300);
		issue.addEvaluations(eval1, eval2, eval3);

		persistenceManager.makePersistent(issue);

		executePost("/get-recent-evaluations", createRecentEvalsRequest(300).toByteArray());
		checkResponse(200);
		RecentEvaluations result = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(0, result.getIssuesCount());
	}

	public void testGetEvaluationsNotAuthenticated() throws Exception {
		executePost("/get-evaluations", GetEvaluations.newBuilder()
				.setSessionId(555)
				.addHashes(encodeHash("fad"))
				.build().toByteArray());
		checkResponse(403, "not authenticated");
	}

	public void testGetEvaluationsForNonexistentIssue() throws Exception {
		createCloudSession(555);

		executePost("/get-evaluations", GetEvaluations.newBuilder()
				.setSessionId(555)
				.addHashes(encodeHash("fad"))
				.build().toByteArray());
		checkResponse(200);
		RecentEvaluations evals = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(0, evals.getIssuesCount());
	}

	public void testGetEvaluationsForSomeNonexistentIssues() throws Exception {
		createCloudSession(555);

		DbIssue issue1 = FlybushServletUtil.createDbIssue("fad1");
		issue1.addEvaluations(FlybushServletUtil.createEvaluation(issue1, "someone1", 100),
							 FlybushServletUtil.createEvaluation(issue1, "someone2", 200),
							 FlybushServletUtil.createEvaluation(issue1, "someone3", 300));

		persistenceManager.makePersistentAll(issue1);

		executePost("/get-evaluations", GetEvaluations.newBuilder()
				.setSessionId(555)
				.addHashes(encodeHash("fad1"))
				.addHashes(encodeHash("fad2"))
				.build().toByteArray());
		checkResponse(200);
		RecentEvaluations evals = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(1, evals.getIssuesCount());
		Issue protoIssue1 = evals.getIssues(0);
		assertEquals(3, protoIssue1.getEvaluationsCount());
		assertEquals(100, protoIssue1.getEvaluations(0).getWhen());
		assertEquals(200, protoIssue1.getEvaluations(1).getWhen());
		assertEquals(300, protoIssue1.getEvaluations(2).getWhen());
	}

	public void testGetEvaluations() throws Exception {
		createCloudSession(555);

		DbIssue issue1 = FlybushServletUtil.createDbIssue("fad1");
		issue1.addEvaluations(FlybushServletUtil.createEvaluation(issue1, "someone1", 100),
							 FlybushServletUtil.createEvaluation(issue1, "someone2", 200),
							 FlybushServletUtil.createEvaluation(issue1, "someone3", 300));

		DbIssue issue2 = FlybushServletUtil.createDbIssue("fad2");
		issue2.addEvaluations(FlybushServletUtil.createEvaluation(issue2, "someone1", 2100),
							 FlybushServletUtil.createEvaluation(issue2, "someone2", 2200),
							 FlybushServletUtil.createEvaluation(issue2, "someone3", 2300));

		persistenceManager.makePersistentAll(issue1, issue2);

		executePost("/get-evaluations",
				GetEvaluations.newBuilder()
					.setSessionId(555)
					.addHashes(encodeHash("fad1"))
					.addHashes(encodeHash("fad2"))
					.build()
				.toByteArray());
		checkResponse(200);
		RecentEvaluations evals = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(2, evals.getIssuesCount());
		Issue protoIssue1 = evals.getIssues(0);
		assertEquals("fad1", decodeHash(protoIssue1.getHash()));
		assertEquals(3, protoIssue1.getEvaluationsCount());
		assertEquals(100, protoIssue1.getEvaluations(0).getWhen());
		assertEquals(200, protoIssue1.getEvaluations(1).getWhen());
		assertEquals(300, protoIssue1.getEvaluations(2).getWhen());

		Issue protoIssue2 = evals.getIssues(1);
		assertEquals("fad2", decodeHash(protoIssue2.getHash()));
		assertEquals(3, protoIssue2.getEvaluationsCount());
		assertEquals(2100, protoIssue2.getEvaluations(0).getWhen());
		assertEquals(2200, protoIssue2.getEvaluations(1).getWhen());
		assertEquals(2300, protoIssue2.getEvaluations(2).getWhen());
	}

	public void testGetEvaluationsOnlyShowsLatestFromEachPerson()
			throws Exception {
		createCloudSession(555);

		DbIssue issue = FlybushServletUtil.createDbIssue("fad1");
		issue.addEvaluations(FlybushServletUtil.createEvaluation(issue, "first", 100),
							 FlybushServletUtil.createEvaluation(issue, "second", 200),
							 FlybushServletUtil.createEvaluation(issue, "first", 300));

		persistenceManager.makePersistentAll(issue);

		executePost("/get-evaluations", GetEvaluations.newBuilder()
				.setSessionId(555)
				.addHashes(encodeHash("fad1"))
				.build().toByteArray());
		checkResponse(200);
		RecentEvaluations evals = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(1, evals.getIssuesCount());
		Issue protoIssue = evals.getIssues(0);
		assertEquals(2, protoIssue.getEvaluationsCount());
		assertEquals(200, protoIssue.getEvaluations(0).getWhen());
		assertEquals(300, protoIssue.getEvaluations(1).getWhen());
	}

	// ========================= end of tests ================================

    private void checkAllFieldsAreBlank(Issue protoIssue1) {
        assertFalse(protoIssue1.hasFirstSeen());
        assertFalse(protoIssue1.hasLastSeen());
        assertEquals(0, protoIssue1.getEvaluationsCount());
        assertFalse(protoIssue1.hasBugPattern());
        assertFalse(protoIssue1.hasHash());
        assertFalse(protoIssue1.hasPriority());
        assertFalse(protoIssue1.hasPrimaryClass());
    }

	private FindIssues.Builder createAuthenticatedFindIssues(String... hashes) {
		return createAuthenticatedFindIssues().addAllMyIssueHashes(encodeHashes(Arrays.asList(hashes)));
	}

	private FindIssues.Builder createAuthenticatedFindIssues() {
		return FindIssues.newBuilder().setSessionId(555);
	}

	private GetRecentEvaluations createRecentEvalsRequest(int timestamp) {
		return GetRecentEvaluations.newBuilder()
				.setSessionId(555)
				.setTimestamp(timestamp)
				.build();
	}

	private void checkEvaluationsEqual(DbEvaluation dbEval, Evaluation protoEval) {
		assertEquals(dbEval.getComment(), protoEval.getComment());
		assertEquals(dbEval.getDesignation(), protoEval.getDesignation());
		assertEquals(dbEval.getWhen(), protoEval.getWhen());
		assertEquals(dbEval.getWho(), protoEval.getWho());
	}

}