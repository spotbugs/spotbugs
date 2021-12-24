package sfBugs;

import java.sql.Connection;
import java.sql.PreparedStatement;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3079260 {
    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
    public PreparedStatement buildGetDataSetStatement(Connection conn) throws Exception {
        PreparedStatement stmt = conn.prepareStatement("select * from blah");
        stmt.execute();
        return stmt;
    }
}
