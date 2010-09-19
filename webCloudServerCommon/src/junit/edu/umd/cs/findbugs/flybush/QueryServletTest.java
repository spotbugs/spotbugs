package edu.umd.cs.findbugs.flybush;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssuesResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetRecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;

import java.io.IOException;
import java.util.Arrays;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.encodeHashes;

@SuppressWarnings({"UnusedDeclaration"})
public abstract class QueryServletTest extends AbstractFlybushServletTest {

    @Override
    protected AbstractFlybushServlet createServlet() {
        return new QueryServlet();
    }

    public void testFindIssuesOneFoundNoEvaluations() throws Exception {
        DbIssue foundIssue = createDbIssue("FAD1");
        getPersistenceManager().makePersistent(foundIssue);

        FindIssuesResponse result = findIssues("FAD1", "FAD2");
        assertEquals(2, result.getFoundIssuesCount());

        checkTerseIssue(result.getFoundIssues(0));
        checkIssueEmpty(result.getFoundIssues(1));
    }

    public void testFindIssuesWithEvaluations() throws Exception {
        DbIssue foundIssue = createDbIssue("fad2");
        DbEvaluation eval = createEvaluation(foundIssue, "someone", 100);

        // apparently the evaluation is automatically persisted. throws
        // exception when attempting to persist the eval with the issue.
        getPersistenceManager().makePersistent(foundIssue);

        FindIssuesResponse result = findIssues("fad1", "fad2");

        assertEquals(2, result.getFoundIssuesCount());
        checkIssueEmpty(result.getFoundIssues(0));
        checkTerseIssue(result.getFoundIssues(1), eval);
    }

    public void testFindIssuesWithOldStyleEvaluation() throws Exception {
        DbIssue foundIssue = createDbIssue("fad2");
        DbEvaluation eval = createEvaluation(foundIssue, "someone", 100);
        persistenceHelper.convertToOldCommentStyleForTesting(eval);

        // apparently the evaluation is automatically persisted. throws
        // exception when attempting to persist the eval with the issue.
        getPersistenceManager().makePersistent(foundIssue);

        FindIssuesResponse result = findIssues("fad1", "fad2");

        assertEquals(2, result.getFoundIssuesCount());
        checkIssueEmpty(result.getFoundIssues(0));
        checkTerseIssue(result.getFoundIssues(1), eval);
    }

    public void testFindIssuesOnlyShowsLatestEvaluationFromEachPerson() throws Exception {
        DbIssue foundIssue = createDbIssue("fad1");
        createEvaluation(foundIssue, "first", 100);
        DbEvaluation eval2 = createEvaluation(foundIssue, "second", 200);
		DbEvaluation eval3 = createEvaluation(foundIssue, "first", 300);

        // apparently the evaluation is automatically persisted. throws
        // exception when attempting to persist the eval with the issue.
        getPersistenceManager().makePersistent(foundIssue);

        FindIssuesResponse result = findIssues("fad2", "fad1");
        assertEquals(2, result.getFoundIssuesCount());

        checkIssueEmpty(result.getFoundIssues(0));
        checkTerseIssue(result.getFoundIssues(1), eval2, eval3);
    }

    //TODO: updated bug links should be included in this list!
    public void testGetRecentEvaluations() throws Exception {
        DbIssue issue = createDbIssue("fad");
        createEvaluation(issue, "someone1", 100);
		DbEvaluation eval2 = createEvaluation(issue, "someone2", 200);
        DbEvaluation eval3 = createEvaluation(issue, "someone3", 300);

        getPersistenceManager().makePersistent(issue);

        executePost("/get-recent-evaluations", createRecentEvalsRequest(150).toByteArray());
        checkResponse(200);
        RecentEvaluations result = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(1, result.getIssuesCount());

        // check issues
        Issue foundissueProto = result.getIssues(0);
        checkIssuesEqualExceptTimestamps(issue, foundissueProto);

        // check evaluations
        assertEquals(2, foundissueProto.getEvaluationsCount());
        checkEvaluationsEqual(eval2, foundissueProto.getEvaluations(0));
		checkEvaluationsEqual(eval3, foundissueProto.getEvaluations(1));
    }

    public void testGetRecentEvaluationsOnlyShowsLatestFromEachPerson() throws Exception {
        DbIssue issue = createDbIssue("fad");
        createEvaluation(issue, "first",  100);
		createEvaluation(issue, "second", 200);
        createEvaluation(issue, "first",  300);
        DbEvaluation eval4 = createEvaluation(issue, "second", 400);
        DbEvaluation eval5 = createEvaluation(issue, "first",  500);

        getPersistenceManager().makePersistent(issue);

        executePost("/get-recent-evaluations", createRecentEvalsRequest(150).toByteArray());
        checkResponse(200);
        RecentEvaluations result = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(1, result.getIssuesCount());

        // check issues
        Issue foundissueProto = result.getIssues(0);
        checkIssuesEqualExceptTimestamps(issue, foundissueProto);

        // check evaluations
        assertEquals(2, foundissueProto.getEvaluationsCount());
        checkEvaluationsEqual(eval4, foundissueProto.getEvaluations(0));
		checkEvaluationsEqual(eval5, foundissueProto.getEvaluations(1));
    }

    public void testGetRecentEvaluationsNoneFound() throws Exception {
        DbIssue issue = createDbIssue("fad");
        createEvaluation(issue, "someone", 100);
		createEvaluation(issue, "someone", 200);
        createEvaluation(issue, "someone", 300);

        getPersistenceManager().makePersistent(issue);

        executePost("/get-recent-evaluations", createRecentEvalsRequest(300).toByteArray());
        checkResponse(200);
        RecentEvaluations result = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(0, result.getIssuesCount());
    }

    // ========================= end of tests ================================

    private FindIssuesResponse findIssues(String... hashes) throws IOException {
        FindIssues findIssues = createUnauthenticatedFindIssues(hashes).build();
        executePost("/find-issues", findIssues.toByteArray());
        return FindIssuesResponse.parseFrom(outputCollector.toByteArray());
    }

    private void checkTerseIssue(Issue issue, DbEvaluation... evals) {
        assertEquals(SAMPLE_TIMESTAMP +100, issue.getFirstSeen());
        assertEquals(SAMPLE_TIMESTAMP +200, issue.getLastSeen());
        assertEquals("http://bug.link", issue.getBugLink());
        assertEquals("JIRA", issue.getBugLinkTypeStr());
        assertFalse(issue.hasBugPattern());
        assertFalse(issue.hasHash());
        assertFalse(issue.hasPriority());
        assertFalse(issue.hasPrimaryClass());

        assertEquals(evals.length, issue.getEvaluationsCount());
        for (int i = 0; i < evals.length; i++) {
            checkEvaluationsEqual(evals[i], issue.getEvaluations(i));
        }
    }

    private void checkIssueEmpty(Issue protoIssue1) {
        assertFalse(protoIssue1.hasFirstSeen());
        assertFalse(protoIssue1.hasLastSeen());
        assertEquals(0, protoIssue1.getEvaluationsCount());
        assertFalse(protoIssue1.hasBugPattern());
        assertFalse(protoIssue1.hasHash());
        assertFalse(protoIssue1.hasPriority());
        assertFalse(protoIssue1.hasPrimaryClass());
    }

    private FindIssues.Builder createUnauthenticatedFindIssues(String... hashes) {
        return FindIssues.newBuilder().addAllMyIssueHashes(encodeHashes(Arrays.asList(hashes)));
    }

    private GetRecentEvaluations createRecentEvalsRequest(int timestamp) {
        return GetRecentEvaluations.newBuilder()
                .setTimestamp(timestamp)
                .build();
	}

    private void checkEvaluationsEqual(DbEvaluation dbEval, Evaluation protoEval) {
        assertEquals(dbEval.getComment(), protoEval.getComment());
        assertEquals(dbEval.getDesignation(), protoEval.getDesignation());
		assertEquals(dbEval.getWhen(), protoEval.getWhen());
        assertEquals(getDbUser(dbEval.getWho()).getEmail(), protoEval.getWho());
    }

}
