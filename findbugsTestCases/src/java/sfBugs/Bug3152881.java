package sfBugs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Bug3152881 {
    void falsePositive(Connection connection)
            throws SQLException {
        Statement createStmt = null;
        PreparedStatement insertStmt = null;
        try {
            createStmt = connection.createStatement();
            createStmt.executeUpdate("CREATE...");

            insertStmt = connection.prepareStatement("INSERT...");
            insertStmt.executeBatch();

        } finally {
            if (createStmt != null)
                createStmt.close();
            if (insertStmt != null) {
                insertStmt.close();
            }
        }
    }
}
