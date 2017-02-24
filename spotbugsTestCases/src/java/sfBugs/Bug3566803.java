package sfBugs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

/** Now Bug1116 */

public class Bug3566803 {

    @ExpectWarning("ODR_OPEN_DATABASE_RESOURCE")
    public void notReported(String url, String username, String password) throws Exception {
        for (int i = 0; i < 10; i++) {
            if (i > 5) {
                Connection connection = DriverManager.getConnection(url, username, password);
                PreparedStatement pstmt = connection.prepareStatement("SELECT count(1) from tab");
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    System.out.println(rs.getString(1));
                }
            }
        }
    }

    @ExpectWarning("ODR_OPEN_DATABASE_RESOURCE")
    public void isReported(String url, String username, String password) throws Exception {
        Connection connection = DriverManager.getConnection(url, username, password);
        PreparedStatement pstmt = connection.prepareStatement("SELECT count(1) from tab");
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            System.out.println(rs.getString(1));
        }
    }

    @DesireWarning("ODR_OPEN_DATABASE_RESOURCE")
    public static void main(String... strings) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
                String driverName = "oracle.jdbc.driver.OracleDriver";
                Class.forName(driverName);
                String url = "";
                String username = "";
                String password = "";
                
                // Bug: We only close the resources opened in the last iteration
                for (int i = 0; i < 10; i++) {
                                connection = DriverManager.getConnection(url, username,
                                                password);

                                pstmt = connection
                                                .prepareStatement("SELECT count(1) from tab");

                                rs = pstmt.executeQuery();
                                while (rs.next()) {
                                        System.out.println(rs.getString(1));
                                }
                }
        } catch (ClassNotFoundException e) {
                e.printStackTrace();
        } catch (SQLException sql) {
                System.out.println("SQLException" + sql.getMessage());
        } finally {
                try {
                        if (rs != null)
                                rs.close();
                } catch (SQLException e) {
                        e.printStackTrace();
                }
                try {
                        if (pstmt != null)
                                pstmt.close();
                } catch (SQLException e) {
                        e.printStackTrace();
                }
                try {
                        if (connection != null)
                                connection.close();
                } catch (SQLException e) {
                        e.printStackTrace();
                }
        }
}


}
