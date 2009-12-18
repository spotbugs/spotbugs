package edu.umd.cs.findbugs.flybush;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

public class Evaluation {
	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

	@Persistent private String who;
	@Persistent private String designation;
	@Persistent private String comment;
	@Persistent private DbIssue issue;
	@Persistent private long when;
	@Persistent private Invocation invocation;

}
