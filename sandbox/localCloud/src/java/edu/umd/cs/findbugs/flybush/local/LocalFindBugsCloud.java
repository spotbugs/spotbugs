package edu.umd.cs.findbugs.flybush.local;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.SimpleLayout;
import org.datanucleus.store.rdbms.SchemaTool;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import javax.jdo.PersistenceManagerFactory;
import java.util.Arrays;

public class LocalFindBugsCloud {
    public static void main(String[] args) throws Exception {

        initLogging();

//        createSchema();

        Server server = new Server(8080);

        server.setHandler(loadWebApp());
        server.start();
        
        for (Connector connector : server.getConnectors()) {
            System.out.println("Server started: " + connector.getName());
        }
        server.join();
    }

    private static void initLogging() {
        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.WARN);
        ConsoleAppender appender = new ConsoleAppender(new SimpleLayout());
        appender.setThreshold(Priority.INFO);
        logger.addAppender(appender);
    }

    private static void createSchema() throws Exception {
        SchemaTool schemaTool = new SchemaTool();
        PersistenceManagerFactory pmf = new LocalPersistenceHelper().getPersistenceManagerFactory();
        schemaTool.createSchema(pmf, Arrays.asList(
                LocalDbEvaluation.class.getSimpleName(),
                LocalDbInvocation.class.getSimpleName(),
                LocalDbIssue.class.getSimpleName(),
                LocalDbUser.class.getSimpleName(),
                LocalSqlCloudSession.class.getSimpleName()
                ));
        pmf.close();
    }

    private static WebAppContext loadWebApp() {
        WebAppContext context = new WebAppContext();
        context.setDescriptor("web-root/WEB-INF/web.xml");
        context.setResourceBase("web-root");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        return context;
    }
}
