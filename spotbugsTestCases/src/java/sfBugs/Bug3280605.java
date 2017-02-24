package sfBugs;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3280605 {
    private static int dataColX = 1;
    private static int dateColX = 3;

    @NoWarning("SQL_BAD_RESULTSET_ACCESS")
    int getValue(ResultSet rs) throws SQLException {
        int total = 0;

        while (rs.next()) {
            int key = rs.getInt(dataColX);

            String transitionDate = rs.getString(dateColX);
            total += key;
            total += transitionDate.length();
        }

        return total;
    }

}
