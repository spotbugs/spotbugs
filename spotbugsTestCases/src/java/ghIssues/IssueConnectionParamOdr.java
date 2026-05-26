package ghIssues;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Regression for JDBC resources created from a Connection method parameter.
 */
public class IssueConnectionParamOdr {

    public void statementFromConnectionParameter(Connection conn) throws SQLException {
        Statement statement = conn.prepareStatement("SELECT 1");
        statement.executeQuery("SELECT 1");
    }
}
