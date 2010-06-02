package edu.umd.cs.findbugs.flybush;

import javax.jdo.PersistenceManager;

public interface FlybushServletTestHelper {
    void setUp() throws Exception;

    void tearDown() throws Exception;

    PersistenceManager getPersistenceManager();

    void initPersistenceManager();

    PersistenceHelper createPersistenceHelper(PersistenceManager persistenceManager);
}
