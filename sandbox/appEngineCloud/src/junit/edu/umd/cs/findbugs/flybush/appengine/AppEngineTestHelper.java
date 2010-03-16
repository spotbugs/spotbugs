package edu.umd.cs.findbugs.flybush.appengine;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import edu.umd.cs.findbugs.flybush.FlybushServletTestHelper;
import edu.umd.cs.findbugs.flybush.PersistenceHelper;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import java.io.File;
import java.util.Properties;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

public class AppEngineTestHelper implements FlybushServletTestHelper {
    private PersistenceManager persistenceManager;
    private PersistenceManager actualPersistenceManager;

    public void setUp() throws Exception {
        TestEnvironment testEnvironment = new TestEnvironment();
        ApiProxy.setEnvironmentForCurrentThread(testEnvironment);
        ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(".")) {});
        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        proxy.setProperty(LocalDatastoreService.NO_STORAGE_PROPERTY, Boolean.TRUE.toString());
        initPersistenceManager();
    }

    public void tearDown() throws Exception {
        actualPersistenceManager.close();
        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        LocalDatastoreService datastoreService =
                (LocalDatastoreService) proxy.getService(LocalDatastoreService.PACKAGE);
        datastoreService.clearProfiles();
        ApiProxy.setDelegate(null);
        ApiProxy.setEnvironmentForCurrentThread(null);
    }

    /**
     * Creates a PersistenceManagerFactory on the fly, with the exact same information
     * stored in the /WEB-INF/META-INF/jdoconfig.xml file.
     */
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

    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public PersistenceHelper createPersistenceHelper(final PersistenceManager persistenceManager) {
        return new AppEnginePersistenceHelper() {
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