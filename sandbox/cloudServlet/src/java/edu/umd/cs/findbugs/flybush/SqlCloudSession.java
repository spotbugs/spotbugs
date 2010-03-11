package edu.umd.cs.findbugs.flybush;

import javax.jdo.annotations.*;
import java.util.Date;

public interface SqlCloudSession {

    String getRandomID();

    void setInvocation(DbInvocation invocation);

    Object getUser();

    Object getInvocation();
}
