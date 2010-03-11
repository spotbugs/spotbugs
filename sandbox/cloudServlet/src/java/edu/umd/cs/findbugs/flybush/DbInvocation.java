package edu.umd.cs.findbugs.flybush;

public interface DbInvocation {

    long getStartTime();

    void setStartTime(long startTime);

    Object getWho();

    void setWho(Object user);
}
