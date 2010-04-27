package edu.umd.cs.findbugs.cloud.appEngine;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAppEngineCloudTest extends TestCase {
    protected static final long SAMPLE_DATE = 1200000000L * 1000L; // Thu, 10 Jan 2008 21:20:00 GMT

    protected BugInstance missingIssue;
    protected BugInstance foundIssue;
    protected boolean addMissingIssue;

    @Override
	protected void setUp() throws Exception {
		missingIssue = new BugInstance("MISSING", 2).addClass("MissingClass");
        missingIssue.setInstanceHash("fad1");
        foundIssue = new BugInstance("FOUND", 2).addClass("FoundClass");
        foundIssue.setInstanceHash("fad2");
        addMissingIssue = false;
	}

    protected ProtoClasses.Issue createFoundIssueProto() {
        return ProtoClasses.Issue.newBuilder()
                .setFirstSeen(SAMPLE_DATE+100)
                .setLastSeen(SAMPLE_DATE+200)
                .addEvaluations(ProtoClasses.Evaluation.newBuilder()
                        .setWho("commenter")
                        .setWhen(SAMPLE_DATE+300)
                        .setComment("my comment")
                        .setDesignation("NEEDS_STUDY")
                        .build())
                .addEvaluations(ProtoClasses.Evaluation.newBuilder()
                        .setWho("test@example.com")
                        .setWhen(SAMPLE_DATE+400)
                        .setComment("later comment")
                        .setDesignation("NOT_A_BUG")
                        .build())
                .addEvaluations(ProtoClasses.Evaluation.newBuilder()
                        .setWho("test@example.com")
                        .setWhen(SAMPLE_DATE+500)
                        .setComment("latest comment")
                        .setDesignation("MUST_FIX")
                        .build())
                .build();
    }

    protected ByteArrayOutputStream setupResponseCodeAndOutputStream(HttpURLConnection connection)
            throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(connection.getResponseCode()).thenReturn(200);
        when(connection.getOutputStream()).thenReturn(outputStream);
        return outputStream;
    }

    protected MockAppEngineCloudClient createAppEngineCloudClient(HttpURLConnection... connections) throws IOException {
        SortedBugCollection bugs = new SortedBugCollection();
        if (addMissingIssue)
            bugs.add(missingIssue);
        bugs.add(foundIssue);
        final Iterator<HttpURLConnection> mockConnections = Arrays.asList(connections).iterator();
        CloudPlugin plugin = new CloudPlugin("AppEngineCloudMiscTests", AppEngineCloudClient.class.getClassLoader(),
                                             AppEngineCloudClient.class, AppEngineNameLookup.class,
                                             new PropertyBundle(), "none", "none");
        Executor executor = Executors.newCachedThreadPool();
        return new MockAppEngineCloudClient(plugin, bugs, executor, mockConnections);
    }

    protected InputStream createFindIssuesResponse(ProtoClasses.Issue foundIssue) {
        ProtoClasses.FindIssuesResponse.Builder issueList = ProtoClasses.FindIssuesResponse.newBuilder();
        if (addMissingIssue)
            issueList.addFoundIssues(ProtoClasses.Issue.newBuilder().build());

        issueList.addFoundIssues(foundIssue);
        return new ByteArrayInputStream(issueList.build().toByteArray());
    }

    protected HttpURLConnection createResponselessConnection() throws IOException {
        final HttpURLConnection conn = mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(conn);
        return conn;
    }

    protected <E> List<E> newList(Iterable<E> iterable) {
        List<E> result = new ArrayList<E>();
        for (E item : iterable) {
            result.add(item);
        }
		return result;
	}
}
