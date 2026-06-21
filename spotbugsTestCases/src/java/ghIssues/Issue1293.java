package ghIssues;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/1293">#1293</a>.
 */
public class Issue1293 {

    public List<String> getList() throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection connection = getConnection();
        Statement statement = getStatement(connection);
        ResultSet resultSet = getResultSet(statement);

        List<String> results = new ArrayList<>();
        while (resultSet.next()) {
            results.add(resultSet.getString("ename"));
        }
        return results;
    }

    private ResultSet getResultSet(Statement statement) throws SQLException {
        return statement.executeQuery("select * from emp");
    }

    private Statement getStatement(Connection connection) throws SQLException {
        return connection.createStatement();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/example", "example", "example");
    }
}
