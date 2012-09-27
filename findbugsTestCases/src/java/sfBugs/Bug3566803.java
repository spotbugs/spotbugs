package sfBugs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Bug3566803 {

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

    public void isReported(String url, String username, String password) throws Exception {
        Connection connection = DriverManager.getConnection(url, username, password);
        PreparedStatement pstmt = connection.prepareStatement("SELECT count(1) from tab");
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            System.out.println(rs.getString(1));
        }
    }

}
