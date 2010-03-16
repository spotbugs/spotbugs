package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.FlybushServletTestHelper;
import edu.umd.cs.findbugs.flybush.PersistenceHelper;
import org.mockito.Mockito;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import java.util.Arrays;
import java.util.Properties;

class LocalFlybushServletTestHelper implements FlybushServletTestHelper {
    public PersistenceManager actualPersistenceManager;
    public PersistenceManager persistenceManager;
    private PersistenceManagerFactory pmf;
    private static long i = 0;

    public void setUp() throws Exception {
        initPersistenceManager();
    }

    public void tearDown() throws Exception {
        actualPersistenceManager.close();
        pmf.close();
    }

    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public void initPersistenceManager() {
        Properties newProperties = new Properties();
        newProperties.put("datanucleus.ConnectionDriverName", "org.hsqldb.jdbcDriver");
        newProperties.put("javax.jdo.PersistenceManagerFactoryClass",
                          "org.datanucleus.jdo.JDOPersistenceManagerFactory");
        // each test uses its own DB - not sure how else to do this; shutdown=true breaks the tests
        String dbName = "tests" + i++;
        newProperties.put("javax.jdo.option.ConnectionURL", "jdbc:hsqldb:mem:" + dbName);
        newProperties.put("javax.jdo.option.ConnectionUserName", "sa");
        newProperties.put("javax.jdo.option.ConnectionPassword", "");
        newProperties.put("javax.jdo.option.NontransactionalRead", "true");
        newProperties.put("javax.jdo.option.NontransactionalWrite", "true");
        newProperties.put("javax.jdo.option.RetainValues", "true");
        newProperties.put("datanucleus.autoCreateTables", "true");
        pmf = JDOHelper.getPersistenceManagerFactory(newProperties);
        if (actualPersistenceManager != null) {
            System.out.println("OLD PERS MSGG");
        }
        actualPersistenceManager = pmf.getPersistenceManager();

        persistenceManager = Mockito.spy(actualPersistenceManager);
        Mockito.doNothing().when(persistenceManager).close();
    }

    public PersistenceHelper createPersistenceHelper(final PersistenceManager persistenceManager) {
        return new LocalPersistenceHelper() {
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
