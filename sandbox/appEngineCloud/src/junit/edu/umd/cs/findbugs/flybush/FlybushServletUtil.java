package edu.umd.cs.findbugs.flybush;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import junit.framework.Assert;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.decodeHash;

public class FlybushServletUtil {
    private FlybushServletUtil() { }

    public static DbEvaluation createEvaluation(DbIssue issue, String who, int when) {
        DbEvaluation eval = new DbEvaluation();
        eval.setComment("my comment");
        eval.setDesignation("MUST_FIX");
        eval.setIssue(issue);
        eval.setWhen(when);
        eval.setWho(who);
        return eval;
    }

    public static DbIssue createDbIssue(String patternAndHash) {
        patternAndHash = AppEngineProtoUtil.normalizeHash(patternAndHash);
        DbIssue foundIssue = new DbIssue();
        foundIssue.setHash(patternAndHash);
        foundIssue.setBugPattern(patternAndHash);
        foundIssue.setPriority(2);
        foundIssue.setPrimaryClass("my.class");
        foundIssue.setFirstSeen(100);
        foundIssue.setLastSeen(200);
        return foundIssue;
    }

    public static void checkIssuesEqualExceptTimestamps(DbIssue dbIssue, Issue protoIssue) {
        Assert.assertEquals(dbIssue.getHash(), decodeHash(protoIssue.getHash()));
        Assert.assertEquals(dbIssue.getBugPattern(), protoIssue.getBugPattern());
        Assert.assertEquals(dbIssue.getPriority(), protoIssue.getPriority());
        Assert.assertEquals(dbIssue.getPrimaryClass(), protoIssue.getPrimaryClass());
    }
}
