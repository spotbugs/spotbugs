package edu.umd.cs.findbugs.flybush;

public interface SqlCloudSession {

    String getRandomID();

    void setInvocation(DbInvocation invocation);

    Object getUser();

    String getEmail();

    Object getInvocation();
}
