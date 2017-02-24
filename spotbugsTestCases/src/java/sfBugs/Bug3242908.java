package sfBugs;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Bug3242908 {
    void getTableStructure(Connection conn, Statement statement, DatabaseMetaData meta) throws Exception {
        Statement stmt = null;
        ResultSet select_rs = null;
        ResultSet col = null;
        String s = null;

        try {
            stmt = conn.createStatement();
            select_rs = statement.executeQuery("aa");
            col = meta.getColumns("aa", "aa", "aa", null);
            s = "aaa";
        } catch (SQLException eSchema) {
        }

        try {
            stmt.close();
            select_rs.close();
            col.close();
            s.toCharArray();
        } catch (SQLException e) {
        }
    }

}
