package edu.umd.cs.findbugs.flybush.appengine;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import edu.umd.cs.findbugs.flybush.FlybushServletTestHelper;


public class AppEngineUpdateTestHelper 
        implements FlybushServletTestHelper<AppEngineUsagePersistenceHelper> {
    private PersistenceManager persistenceManager;

    private PersistenceManager actualPersistenceManager;

    private LocalServiceTestHelper helper;

    @Override
    public void setUp() throws Exception {
        LocalDatastoreServiceTestConfig config = new LocalDatastoreServiceTestConfig();
        config.setNoStorage(true);
        helper = new LocalServiceTestHelper(config);
        helper.setUp();
        initPersistenceManager();
    }

    @Override
    public void tearDown() throws Exception {
        helper.tearDown();
    }

    /**
     * Creates a PersistenceManagerFactory on the fly, with the exact same
     * information stored in the /WEB-INF/META-INF/jdoconfig.xml file.
     */
    @Override
    public void initPersistenceManager() {
        Properties newProperties = new Properties();
        newProperties.put("javax.jdo.PersistenceManagerFactoryClass",
                "org.datanucleus.store.appengine.jdo.DatastoreJDOPersistenceManagerFactory");
        newProperties.put("javax.jdo.option.ConnectionURL", "appengine");
        newProperties.put("javax.jdo.option.NontransactionalRead", "true");
        newProperties.put("javax.jdo.option.NontransactionalWrite", "true");
        newProperties.put("javax.jdo.option.RetainValues", "true");
        newProperties.put("datanucleus.appengine.autoCreateDatastoreTxns", "true");
        PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(newProperties);
        actualPersistenceManager = pmf.getPersistenceManager();
        persistenceManager = spy(actualPersistenceManager);
        doNothing().when(persistenceManager).close();
    }

    @Override
    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }


    @Override
    public AppEngineUsagePersistenceHelper createPersistenceHelper(final PersistenceManager persistenceManager) {
        return new AppEngineUsagePersistenceHelper() {
            @Override
            public PersistenceManagerFactory getPersistenceManagerFactory() {
                throw new UnsupportedOperationException();
            }

            @Override
            public PersistenceManager getPersistenceManager() {
                return persistenceManager;
            }
        };
    }
}