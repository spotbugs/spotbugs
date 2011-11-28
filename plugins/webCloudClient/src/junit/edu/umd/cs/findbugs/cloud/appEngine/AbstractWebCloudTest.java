package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.mockito.Mockito;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.CloudPluginBuilder;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.username.WebCloudNameLookup;

public abstract class AbstractWebCloudTest extends TestCase {
    protected static final long SAMPLE_DATE = 1200000000L * 1000L; // Thu, 10
                                                                   // Jan 2008
                                                                   // 21:20:00
                                                                   // GMT

    protected BugInstance missingIssue;

    protected BugInstance foundIssue;

    protected boolean addMissingIssue;

    protected SortedBugCollection bugCollection;

    private CloudPlugin plugin;

    private ConsoleHandler logHandler;

    @Override
    protected void setUp() throws Exception {
        missingIssue = new BugInstance("MISSING", 2).addClass("MissingClass");
        missingIssue.setInstanceHash("fad1");
        foundIssue = new BugInstance("FOUND", 2).addClass("FoundClass");
        foundIssue.setInstanceHash("fad2");
        addMissingIssue = false;
        bugCollection = new SortedBugCollection();
        plugin = new CloudPluginBuilder().setCloudid("AbstractWebCloudTest")
                .setClassLoader(WebCloudClient.class.getClassLoader()).setCloudClass(WebCloudClient.class)
                .setUsernameClass(WebCloudNameLookup.class).setProperties(new PropertyBundle()).setDescription("none")
                .setDetails("none").createCloudPlugin();
        if (true) {
            Logger logger = Logger.getLogger("edu.umd.cs.findbugs.cloud");
            logger.setLevel(Level.FINEST);
            logHandler = new ConsoleHandler();
            logHandler.setLevel(Level.FINER);
            logHandler.setFilter(new Filter() {
                public boolean isLoggable(LogRecord record) {
                    return record.getLevel().intValue() < Level.INFO.intValue();
                }
            });
            logger.addHandler(logHandler);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Logger logger = Logger.getLogger("edu.umd.cs.findbugs.cloud");
        logger.removeHandler(logHandler);
    }

    protected ProtoClasses.Issue createFoundIssueProto() {
        return ProtoClasses.Issue
                .newBuilder()
                .setFirstSeen(SAMPLE_DATE + 100)
                .setLastSeen(SAMPLE_DATE + 200)
                .addEvaluations(
                        ProtoClasses.Evaluation.newBuilder().setWho("commenter").setWhen(SAMPLE_DATE + 300)
                                .setComment("my comment").setDesignation("NEEDS_STUDY").build())
                .addEvaluations(
                        ProtoClasses.Evaluation.newBuilder().setWho("test@example.com").setWhen(SAMPLE_DATE + 400)
                                .setComment("later comment").setDesignation("NOT_A_BUG").build())
                .addEvaluations(
                        ProtoClasses.Evaluation.newBuilder().setWho("test@example.com").setWhen(SAMPLE_DATE + 500)
                                .setComment("latest comment").setDesignation("MUST_FIX").build()).build();
    }

    protected static ByteArrayOutputStream setupResponseCodeAndOutputStream(HttpURLConnection connection) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Mockito.when(connection.getResponseCode()).thenReturn(200);
        Mockito.when(connection.getOutputStream()).thenReturn(outputStream);
        return outputStream;
    }

    protected MockWebCloudClient createWebCloudClient(HttpURLConnection... connections) throws IOException {
        if (addMissingIssue)
            bugCollection.add(missingIssue);
        bugCollection.add(foundIssue);
        return new MockWebCloudClient(plugin, bugCollection, Arrays.asList(connections));
    }

    protected InputStream createFindIssuesResponse(ProtoClasses.Issue foundIssue, boolean addMissingIssue) {
        return new ByteArrayInputStream(createFindIssuesResponseObj(foundIssue, addMissingIssue).toByteArray());
    }

    protected ProtoClasses.FindIssuesResponse createFindIssuesResponseObj(ProtoClasses.Issue foundIssue, boolean addMissingIssue) {
        ProtoClasses.FindIssuesResponse.Builder issueList = ProtoClasses.FindIssuesResponse.newBuilder();
        if (addMissingIssue)
            issueList.addFoundIssues(ProtoClasses.Issue.newBuilder().build());

        issueList.addFoundIssues(foundIssue);
        return issueList.build();
    }

    protected <E> List<E> newList(Iterable<E> iterable) {
        List<E> result = new ArrayList<E>();
        for (E item : iterable) {
            result.add(item);
        }
        return result;
    }
}
