package edu.umd.cs.findbugs.flybush.appengine;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

public final class PMF {
    private static final Logger log = Logger.getLogger(PMF.class.getName());
    private static final PersistenceManagerFactory pmfInstance;
    static {
        try {
            pmfInstance =  JDOHelper.getPersistenceManagerFactory("transactions-optional");
        } catch (RuntimeException t) {
            log.log(Level.SEVERE, "Runtime exception getting pmf", t);
            throw t;
        } catch (Error t) {
            log.log(Level.SEVERE, "Error getting pmf", t);
            throw t;
        }
    }

    private PMF() {
    }

    public static PersistenceManagerFactory get() {
        return pmfInstance;
    }
}
