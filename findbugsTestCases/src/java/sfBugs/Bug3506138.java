package sfBugs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import edu.umd.cs.findbugs.annotations.DesireWarning;

public class Bug3506138 {

    public static Connection getConnection() {
        throw new UnsupportedOperationException();
    }

    @DesireWarning("ODR_OPEN_DATABASE_RESOURCE")
    public static void main(String[] args) throws Exception {
        Connection conn;
        PreparedStatement pstm = null;
        try {
            conn = getConnection();
            pstm = conn.prepareStatement("123");
            pstm.executeUpdate();
        } finally {
            if (pstm != null)
                pstm.close();
        }
    }
}
