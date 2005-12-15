import java.sql.*;

class SqlInjection {

	ResultSet f(Connection conn, String query) throws Exception {
		Statement statement = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		return statement.executeQuery(query);
	}

	ResultSet g(Connection conn) throws Exception {
		Statement statement = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		return statement.executeQuery("FOOBAR");
	}

	ResultSet h(Connection conn, String name) throws Exception {
		Statement statement = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		return statement.executeQuery("FOO '" + name + "'");
	}
}
