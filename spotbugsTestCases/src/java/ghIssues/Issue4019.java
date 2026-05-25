package ghIssues;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4019">#4019</a>.
 */
public class Issue4019 {
    private static final String DB_URL = "jdbc:h2:mem:testdb";

    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION,ODR_OPEN_DATABASE_RESOURCE")
    public void fetchUserBefore(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
            ps.setInt(1, id);
            ps.executeQuery();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION,ODR_OPEN_DATABASE_RESOURCE")
    public void fetchUserAfter(int id) throws SQLException {
        Connection conn = null;
        Statement ps = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
            ((PreparedStatement) ps).setInt(1, id);
            ((PreparedStatement) ps).executeQuery();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
}
