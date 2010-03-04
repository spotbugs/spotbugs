package edu.umd.cs.findbugs.flybush.local;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable 
public class Thing {
    @Persistent
    @PrimaryKey
    private long id;

    @Persistent
    private String name;

    public Thing(long id, String name) {
        this.id = id;
        this.name = name;
    }
}
