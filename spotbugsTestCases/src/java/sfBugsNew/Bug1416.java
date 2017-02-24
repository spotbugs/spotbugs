package sfBugsNew;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1416 {
    @ExpectWarning(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", confidence = Confidence.HIGH)
    public static boolean hasUser(Connection c, String name) throws SQLException {
        return hasResult(c, "SELECT * FROM users WHERE name='"+name+"'");
    }
    
    // passthru method: warning is generated on call site
    @NoWarning("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public static boolean hasResult(Connection c, String query) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = logAndExecute(query, st)) {
            return rs.next();
        }
    }

    // passthru method: warning is generated on call site
    @NoWarning("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public static ResultSet logAndExecute(String query, Statement st) throws SQLException {
        System.out.println("Executing "+query+"...");
        return st.executeQuery(query);
    }

    // passthru method, but additional execute is dangerous
    @ExpectWarning("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public static ResultSet logAndExecute2(String query, Statement st) throws SQLException {
        st.execute("INSERT INTO logs(type,message) VALUES('query', '"+query+"')");
        return st.executeQuery(query);
    }
}
