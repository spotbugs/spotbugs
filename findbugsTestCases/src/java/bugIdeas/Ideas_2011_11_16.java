package bugIdeas;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public abstract class Ideas_2011_11_16 {
    
    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
    ResultSet doQuery(String query) throws SQLException {
        Connection conn = getConnection();
        Statement statement = conn.createStatement();
        return statement.executeQuery(query);
    }

    abstract Connection getConnection();
}
