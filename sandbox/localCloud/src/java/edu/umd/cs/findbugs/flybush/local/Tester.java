package edu.umd.cs.findbugs.flybush.local;

import org.datanucleus.store.rdbms.SchemaTool;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import java.util.Arrays;
import java.util.Properties;

public class Tester {
    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("javax.jdo.PersistenceManagerFactoryClass",
                               "org.datanucleus.jdo.JDOPersistenceManagerFactory");
        properties.setProperty("javax.jdo.option.ConnectionDriverName", "org.hsqldb.jdbcDriver");
        properties.setProperty("javax.jdo.option.ConnectionURL", "jdbc:hsqldb:file:testdb");
        properties.setProperty("javax.jdo.option.ConnectionUserName", "sa");
        properties.setProperty("javax.jdo.option.ConnectionPassword", "");
        properties.setProperty("javax.jdo.option.NontransactionalRead", "true");
        properties.setProperty("javax.jdo.option.NontransactionalWrite", "true");
        properties.setProperty("datanucleus.autoCreateTables", "true");
        PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(properties);
        SchemaTool schemaTool = new SchemaTool();
        schemaTool.createSchema(pmf, Arrays.asList(Thing.class.getSimpleName()));
        PersistenceManager pm = pmf.getPersistenceManager();
//        pm.makePersistent(new Thing(5, "hello"));
        Object results = pm.newQuery("select from " + Thing.class.getName()).execute();
        System.out.println(results);
        pm.close();
        pmf.close();


        Server server = new Server(8081);

        WebAppContext context = new WebAppContext();
//        context.setDescriptor("C:/Users/Keith/Code/findbugs/sandbox/appEngineCloud/war/WEB-INF/web.xml");
//        context.setResourceBase("C:/Users/Keith/Code/findbugs/sandbox/appEngineCloud/war");
        context.setWar("C:/Users/Keith/Code/Libraries/jetty-distribution-7.0.1.v20091125/webapps/test.war");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        server.setHandler(context);
        System.out.println(server.dump());
        server.start();
        System.out.println("Server started: " + server.dump());
        Thread.sleep(5000);
        System.out.println("Server started: " + server.dump());
        server.join();
    }
}
