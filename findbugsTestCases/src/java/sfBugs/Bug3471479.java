package sfBugs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3471479 {

    
    @DesireNoWarning("OBL_UNSATISFIED_OBLIGATION")
    public String foo(Connection conn, String sql, String id) {
        String status = null;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                status = rs.getString(1);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return status;
    }
}
