package findbugsDiscuss;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Bug20070301 {

    public static void falsePositive( String url,
            String[] schemas) {
                    Connection conn = null;
                    Statement stm  = null;
                    try {
                        conn = DriverManager.getConnection(url);
                        stm = conn.createStatement();

                        for (String schema : schemas) {
                            String sql = "create schema " + schema + ";";
                            stm.executeUpdate(sql);
                        }
                    } catch (SQLException e) {
                        log("exception", e);
                    } finally {
                         if (stm != null)
                            try {
                                stm.close();
                            } catch (SQLException e1) {
                                log("exception", e1);
                            }
                        if (null != conn) {
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                log("exception", e);
                            }
                        }
                    }
                }

    public static void closeQuietly(Closeable e)  {
        if (e != null)
            try {
                e.close();
            } catch (IOException e1) {
              log("Could not close", e1);
            }
    }
    public static void closeQuietly(Statement e){
        if (e != null)
            try {
                e.close();
            } catch (Exception e1) {
              log("Could not close", e1);
            }
    }
    public static void closeQuietly(Connection e) {
        if (e != null)
            try {
                e.close();
            } catch (Exception e1) {
              log("Could not close", e1);
            }
    }
    
    public static void log(String msg, Exception e) {
        System.out.println(msg);
    }

}
