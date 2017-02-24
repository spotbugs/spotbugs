import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Contributed by Matt Hargett, http://www.clock.org/~matt

public class NoSqlInjection {
    private static final String[] COLUMNS = new String[] { "ID", "NAME" };

    private static final String[] PREFIXES = new String[] { "GOPHER", "LLAMA" };

    private Connection connection;

    public NoSqlInjection(Connection connection) {
        this.connection = connection;
    }

    public void beSafe() throws SQLException {
        Statement query = connection.createStatement();
        query.executeQuery("select * from ANIMAL");
        ResultSet zooResults = query.getResultSet();

        String columnString = "TYPE, ANIMAL_ID";
        for (String handlerColumn : COLUMNS) {
            columnString += ", " + handlerColumn;
        }
        while (zooResults.next()) {
            for (String prefix : PREFIXES) {
                String valuesString = "";
                if (prefix.equals("GOPHER_")) {
                    valuesString += "'PLATYPUS'";
                } else if (prefix.equals("LLAMA_")) {
                    valuesString += "'DOLLY'";
                }

                valuesString += "," + prefix;

                for (String column : COLUMNS) {
                    valuesString += "," + column;
                }

                connection.createStatement().executeUpdate(
                        "insert into HANDLER (" + columnString + ") VALUES (" + valuesString + ");");
            }
        }
    }

    public void beSafeSimple() throws SQLException {
        Statement query = connection.createStatement();
        query.executeQuery("select * from ANIMAL");
        ResultSet zooResults = query.getResultSet();

        String columnString = "TYPE, ANIMAL_ID";
        connection.createStatement().executeUpdate("insert into HANDLER (" + columnString + ") VALUES (" + PREFIXES[0] + ");");
    }
}
