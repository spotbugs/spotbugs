package sfBugs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3506138 {

    public static Connection getConnection() {
        throw new UnsupportedOperationException();
    }

    @ExpectWarning("ODR_OPEN_DATABASE_RESOURCE")
    public static void test0(String url) throws Exception {
        Connection conn;
        PreparedStatement pstm = null;
        try {
            conn = DriverManager.getConnection(url);
            pstm = conn.prepareStatement("123");
            pstm.executeUpdate();
        } finally {
            if (pstm != null)
                pstm.close();
        }
    }
    
    @DesireWarning("ODR_OPEN_DATABASE_RESOURCE")
    public static void test1() throws Exception {
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
