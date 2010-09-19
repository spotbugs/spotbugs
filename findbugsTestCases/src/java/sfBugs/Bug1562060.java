package sfBugs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Bug1562060 {

    void f4(Connection conn, String query) throws SQLException {
        PreparedStatement preparedStmt = conn.prepareStatement(query);
        preparedStmt.execute();
    }

    void f5(Connection conn, String query) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute(query);
    }

    void falseNegative(Connection conn, String query) throws SQLException {
        PreparedStatement preparedStmt = null;
        try {

            preparedStmt = conn.prepareStatement(query);
        } finally {
            if (null != preparedStmt) {
                try {
                    preparedStmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();

                }

            }
        }
    }

    void f2(Connection conn, String query) throws SQLException {
        PreparedStatement preparedStmt = null;
        try {

            preparedStmt = conn.prepareStatement(query);
        } finally {
            if (preparedStmt != null) {
                try {
                    preparedStmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();

                }

            }
        }
    }

    void f3(Connection conn, String query) throws SQLException {
        PreparedStatement preparedStmt = null;
        try {

            preparedStmt = conn.prepareStatement(query);
        } finally {
            if (preparedStmt == null) {
                try {
                    preparedStmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();

                }

            }
        }
    }

}
