package ghIssues;

import java.sql.DriverManager;

import edu.umd.cs.findbugs.annotations.CreatesObligation;

public abstract class Issue3170MyConnection implements java.sql.Connection {

    @CreatesObligation
    public static Issue3170MyConnection connect() throws Exception {
        return (Issue3170MyConnection) DriverManager.getConnection("my://foo");
    }

    public void leak() throws Exception {
        Issue3170MyConnection connection = Issue3170MyConnection.connect();
        System.out.println(connection);
    }
}
