package sfBugs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Bug2948672 {
    public static int getRowsCount(Connection dbConnection, String tableName) {
        String sqlStatementString;
        Statement sqlStatement;
        ResultSet rs;
        int rowCount = 0;

        try {
            sqlStatement = dbConnection.createStatement();
            sqlStatementString = "SELECT COUNT(*) FROM " + tableName;
            // System.out.println(sqlStatementString);

            rs = sqlStatement.executeQuery(sqlStatementString);
            rs.next();
            rowCount = rs.getInt(1);

            rs.close();
            sqlStatement.close();
            return rowCount;
        } catch (SQLException e) {
            return rowCount;
        }
    }

}
