package bugIdeas;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class Ideas_2011_11_16 {

    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
    ResultSet doQuery(String query) throws SQLException {
        Connection conn = getConnection();
        Statement statement = conn.createStatement();
        return statement.executeQuery(query);
    }

    @NoWarning("OBL")
    ResultSet doQuery2(Connection conn, String query) throws SQLException {
        Statement statement = conn.createStatement();
        try {
            return statement.executeQuery(query);
        } catch (SQLException e) {
            statement.close();
            throw e;
        }
    }

    ResultSet doQuery3( String query) throws SQLException {
        Connection conn = getConnection();
        Statement statement = conn.createStatement();
        try {
            return statement.executeQuery(query);
        } catch (SQLException e) {
            statement.close();
            throw e;
        }
    }

    @ExpectWarning("OBL")
    int doQuery4( String query) throws SQLException {
        Connection conn = getConnection();
        Statement statement = conn.createStatement();
        try {
            ResultSet rs =  statement.executeQuery(query);
            if (rs.next()) 
                return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            statement.close();
            throw e;
        }
    }
    abstract Connection getConnection();
}
