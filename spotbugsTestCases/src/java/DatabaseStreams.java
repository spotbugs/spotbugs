import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseStreams {

    public static void method() {

        Connection connection = null;
        Statement statement = null;

        try {
            connection = DriverManager.getConnection("blah");
            statement = connection.createStatement();
            // do something with statement
        } catch (SQLException e) {
            System.err.println("Error: " + e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    System.err.println("Error closing statement: " + e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e);
                }
            }
        }
    }
}
