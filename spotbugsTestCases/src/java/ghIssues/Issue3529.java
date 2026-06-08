package ghIssues;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

class Issue3529 {
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/mydatabase";
    private static final String DATABASE_USER = "myuser";
    private boolean passwordDecider = false;

    @ExpectWarning("DMI_CONSTANT_DB_PASSWORD,DMI_EMPTY_DB_PASSWORD")
    public Connection showBugWithTernary() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USER, passwordDecider ? "password" : "");
    }

    @ExpectWarning("DMI_CONSTANT_DB_PASSWORD")
    public Connection showBugWithConstantBranch() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USER, true ? "password" : "unused");
    }
}
