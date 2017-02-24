package sfBugs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Bug3085928 {

    public void relateTagsToRuleset() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String str = null;
        try {
            conn = DriverManager.getConnection("", "", "");

            str = "select * from EMPLOYEE where ID = ? ";
            stmt = conn.prepareStatement(str);
            stmt.setString(1, "");
            rs = stmt.executeQuery();

        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                    rs = null;
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
            try {
                if (stmt != null) {
                    stmt.close();
                    stmt = null;
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}
