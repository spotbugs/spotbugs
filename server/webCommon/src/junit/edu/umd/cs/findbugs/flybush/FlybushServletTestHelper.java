package edu.umd.cs.findbugs.flybush;

import javax.jdo.PersistenceManager;

public interface FlybushServletTestHelper<PersistenceHelper> {
    void setUp() throws Exception;

    void tearDown() throws Exception;

    PersistenceManager getPersistenceManager();

    void initPersistenceManager();

    PersistenceHelper createPersistenceHelper(PersistenceManager persistenceManager);
}
