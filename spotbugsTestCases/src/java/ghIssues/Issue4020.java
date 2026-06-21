package ghIssues;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4020">#4020</a>.
 */
public class Issue4020 {

    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    public void closeWithNullCheck(Connection conn, String sql) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.execute();
        } catch (SQLException ex) {
            // log
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    // log
                }
            }
        }
    }

    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    public void closeWithObjectsNonNull(Connection conn, String sql) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.execute();
        } catch (SQLException ex) {
            // log
        } finally {
            if (Objects.nonNull(stmt)) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    // log
                }
            }
        }
    }
}
