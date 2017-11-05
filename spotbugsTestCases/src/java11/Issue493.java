import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/493">GitHub issue</a>
 */
abstract class Issue493 {
    int method(DataSource ds, String query) throws SQLException {
        try (Connection cnn = ds.getConnection(); Statement st = cnn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            return processQueryResult(rs);
        }
    }

    abstract int processQueryResult(ResultSet rs);
}
