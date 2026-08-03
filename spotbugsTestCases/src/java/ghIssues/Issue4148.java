package ghIssues;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Issue4148 {

    public void prepareCallNotClosed(Connection connection) throws SQLException {
        connection.prepareCall("call procedure()");
    }

    public void prepareCallWithResultSetOptionsNotClosed(Connection connection) throws SQLException {
        connection.prepareCall("call procedure()", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    public void prepareCallWithHoldabilityNotClosed(Connection connection) throws SQLException {
        connection.prepareCall("call procedure()", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    public void prepareCallClosed(Connection connection) throws SQLException {
        try (CallableStatement statement = connection.prepareCall("call procedure()")) {
            statement.execute();
        }
    }
}
