package sfBugs;

import java.sql.Connection;
import java.sql.SQLException;

public class Bug1842545 {

    private static Connection m_connection;

    public static void main(String args[]) {
        Connection c = m_connection; // should trigger warning
    }

    boolean hasConnection() {
        try {
            return (m_connection != null) && !m_connection.isClosed();
        } catch (SQLException ex) // should not trigger warning (?)
        {
            return false;
        }
    }
}
