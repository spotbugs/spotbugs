package sfBugs;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3105145 {
    private static int index = 0;

    private static int FIRST_COLUMN_INDEX = ++index;

    @NoWarning("SQL_BAD_RESULTSET_ACCESS")
    public static String triggerBug(ResultSet resultSet) throws SQLException {

            return resultSet.getString(FIRST_COLUMN_INDEX);
    }

}
