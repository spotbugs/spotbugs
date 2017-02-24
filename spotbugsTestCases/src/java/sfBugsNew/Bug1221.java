package sfBugsNew;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1221 {

    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
    void updateResetFailuresToZero(String ipAddr, Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE resetpasswordfailures SET failureCount=0 WHERE IPAddress=?");
        ps.setString(1, ipAddr);
        ps.executeUpdate();
        conn.close();
        ps.close();
    }

    @NoWarning("OBL")
    void updateResetFailuresToZeroOK(String ipAddr, Connection conn) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement("UPDATE resetpasswordfailures SET failureCount=0 WHERE IPAddress=?");

            ps.setString(1, ipAddr);
            ps.executeUpdate();
        } finally {
            if (ps != null)
                ps.close();
            conn.close();
           
        }
    }
    @NoWarning("OBL")
    void updateResetFailuresToZeroOK2(String ipAddr, Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE resetpasswordfailures SET failureCount=0 WHERE IPAddress=?");
        try {
            ps.setString(1, ipAddr);
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
           
        }
    }

    @DesireNoWarning("OBL")
    void updateResetFailuresToZeroOK3(String ipAddr, Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE resetpasswordfailures SET failureCount=0 WHERE IPAddress=?");
        try {
            ps.setString(1, ipAddr);
            ps.executeUpdate();
        } finally {
            conn.close();
        }
    }

}
