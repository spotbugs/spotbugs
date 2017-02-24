package sfBugs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Bug2831873 {
    public static final String PROCESSLOCK_LOCK_SELECT_QUERY = "SELECT * FROM ProcessLock";

    PreparedStatement foo(Connection conn) throws SQLException {

        PreparedStatement stmt = conn.prepareStatement(PROCESSLOCK_LOCK_SELECT_QUERY, ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);

        return stmt;

    }
}
