package edu.umd.cs.findbugs.flybush;

import java.io.IOException;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

public abstract class BasePersistenceHelper {

    public BasePersistenceHelper() {
        super();
    }

    public abstract PersistenceManagerFactory getPersistenceManagerFactory() throws IOException;

    public abstract PersistenceManager getPersistenceManager() throws IOException;

}