package edu.umd.cs.findbugs.flybush.local;

import java.util.Arrays;

import javax.jdo.PersistenceManagerFactory;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.SimpleLayout;
import org.datanucleus.store.rdbms.SchemaTool;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

public class LocalFindBugsCloud {
    public static void main(String[] args) throws Exception {

        initLogging();

//        createSchema();

        int port = 8080;
        String portOption = System.getProperty("fbcloud.port");
        if (portOption != null)
            port = Integer.parseInt(portOption);
        Server server = new Server(port);

        server.setHandler(loadWebApp());
        server.start();
        
        for (Connector connector : server.getConnectors()) {
            System.out.println("Server started: " + connector.getName());
        }
        server.join();
    }

    private static void initLogging() {
        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.DEBUG);
        ConsoleAppender appender = new ConsoleAppender(new SimpleLayout());
        appender.setThreshold(Priority.DEBUG);
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
