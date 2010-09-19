package bugPatterns;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Bug is to supply constant value 0 for any parameter named columnIndex of a
 * ResultSet
 * 
 */
public class SQL_BAD_RESULTSET_ACCESS {

    void bug1(ResultSet any) throws SQLException {
        any.getString(0);
    }

    void bug2(ResultSet any) throws SQLException {
        any.getInt(0);
    }

    void bug3(ResultSet any) throws SQLException {
        any.getLong(0);
    }

    void bug4(ResultSet any, int anyInt) throws SQLException {
        any.updateInt(0, anyInt);
    }

    void notBug(ResultSet any) throws SQLException {
        any.setFetchSize(0);
    }

    void notBug2(ResultSet any) throws SQLException {
        any.setFetchDirection(0);
    }

    void notBug3(ResultSet any) throws SQLException {
        any.absolute(0);
    }

    void notBug4(ResultSet any) throws SQLException {
        any.relative(0);
    }
}
