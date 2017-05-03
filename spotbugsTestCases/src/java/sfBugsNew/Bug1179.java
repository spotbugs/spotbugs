package sfBugsNew;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1179 {

    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    public void bug(Connection connection) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement("SELECT * FROM Employees");

        } catch (Exception e) {

        } finally {
            if (ps != null)
                ps.close();
        }
    }
}
