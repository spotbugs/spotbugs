package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.local.LocalDbEvaluation;
import edu.umd.cs.findbugs.flybush.local.LocalDbInvocation;
import edu.umd.cs.findbugs.flybush.local.LocalDbIssue;
import edu.umd.cs.findbugs.flybush.local.LocalDbUser;
import edu.umd.cs.findbugs.flybush.local.LocalSqlCloudSession;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.datanucleus.store.rdbms.SchemaTool;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import javax.jdo.PersistenceManagerFactory;
import java.util.Arrays;

public class Tester {
    public static void main(String[] args) throws Exception {
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

        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.ALL);
        logger.addAppender(new ConsoleAppender(new SimpleLayout()));

        Server server = new Server(8080);

        WebAppContext context = new WebAppContext();
        context.setDescriptor("war_exploded/WEB-INF/web.xml");
        context.setResourceBase("war_exploded");
        context.setWar("");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        server.setHandler(context);
        server.start();
        System.out.println("Server started: " + server);
        server.join();
    }
}
