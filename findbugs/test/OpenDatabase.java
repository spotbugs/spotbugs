import java.sql.*;

public class OpenDatabase {
	public void openConnection() throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc url");
	}

	public void openStatement(Connection conn) throws SQLException {
		Statement statement = conn.createStatement();
	}
}

// vim:ts=3
