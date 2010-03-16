package edu.umd.cs.findbugs.flybush;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import junit.framework.Assert;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.decodeHash;

public class FlybushServletTestUtil {
    private FlybushServletTestUtil() { }

    public static DbIssue createDbIssue(String patternAndHash, PersistenceHelper persistenceHelper) {
        patternAndHash = AppEngineProtoUtil.normalizeHash(patternAndHash);
        DbIssue foundIssue = persistenceHelper.createDbIssue();
        foundIssue.setHash(patternAndHash);
        foundIssue.setBugPattern(patternAndHash);
        foundIssue.setPriority(2);
        foundIssue.setPrimaryClass("my.class");
        foundIssue.setFirstSeen(100);
        foundIssue.setLastSeen(200);
        foundIssue.setBugLinkType(DbIssue.DbBugLinkType.JIRA);
        foundIssue.setBugLink("http://bug.link");
        return foundIssue;
    }

    public static void checkIssuesEqualExceptTimestamps(DbIssue dbIssue, Issue protoIssue) {
        Assert.assertEquals(dbIssue.getHash(), decodeHash(protoIssue.getHash()));
        Assert.assertEquals(dbIssue.getBugPattern(), protoIssue.getBugPattern());
        Assert.assertEquals(dbIssue.getPriority(), protoIssue.getPriority());
        Assert.assertEquals(dbIssue.getPrimaryClass(), protoIssue.getPrimaryClass());
        Assert.assertEquals(dbIssue.getBugLink(), protoIssue.hasBugLink() ? protoIssue.getBugLink() : null);
    }
}
