package ghIssues;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3633">#3633</a>.
 */
public class Issue3633 {

    private static final String pwd;

    static {
        pwd = "password";
    }

    public static Connection connectWithStaticBlock() throws SQLException {
        return DriverManager.getConnection("jdbc:hsqldb:mem:test", "sa", pwd);
    }

    private static final String INLINE_PWD = "inline-secret";

    public static Connection connectWithInlineConstant() throws SQLException {
        return DriverManager.getConnection("jdbc:hsqldb:mem:test", "sa", INLINE_PWD);
    }
}
