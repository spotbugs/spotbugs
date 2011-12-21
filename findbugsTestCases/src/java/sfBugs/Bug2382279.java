/* ****************************************
 * $Id$
 * SF bug 2382279:
 *   OBL and ORD false positives on java.sql.Statement.close()
 *
 * JVM:  1.5.0_16 (OS X, PPC)
 * FBv:  1.3.7-dev-20081212
 *
 * Test case based on example code from bug report
 * **************************************** */

package sfBugs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug2382279 {
    private Connection con;

    public Bug2382279(Connection newCon) {
        con = newCon;
    }

    /* ********************
     * Behavior at filing: OBL and ODR false positives The ResultSet object
     * returned by execute() has a method by which the statement that produced
     * it can be retrieved, and so the statement does not need to be closed in
     * this method. As such, no warnings should be thrown.
     * 
     * warnings thrown => M B ODR_OPEN_DATABASE_RESOURCE ODR:
     * sfBugs.Bug2382279.execute(String) \ may fail to close Statement At
     * Bug2382279.java:[line 47]
     * 
     * M X OBL_UNSATISFIED_OBLIGATION OBL: Method \
     * sfBugs.Bug2382279.execute(String) may fail to clean up stream or \
     * resource of type java.sql.Statement Obligation to clean up resource \
     * created at Bug2382279.java:[line 47] is not discharged
     * 
     * ********************
     */
    @DesireNoWarning("ODR")
    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
    public ResultSet execute(String query) throws SQLException {
        // test code adapted from bug report
        ResultSet rs;
        try {
            Statement statement = con.createStatement();
            rs = statement.executeQuery(query);
            return rs;
        } catch (SQLException sqle) {
            System.out.println("Couldn't execute statement");
        }
        return null;
    }

}
