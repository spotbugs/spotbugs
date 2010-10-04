package sfBugs;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Bug3079260 {
    private PreparedStatement buildGetDataSetStatement(Connection conn) throws Exception {
        PreparedStatement stmt = conn.prepareStatement("select * from blah");
        stmt.execute();
        return stmt;
    }
}
