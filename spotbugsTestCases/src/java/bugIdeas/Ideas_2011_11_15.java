package bugIdeas;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Ideas_2011_11_15 {
    public int foo(PreparedStatement stmt) throws Exception {
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery();
            rs.next();
            int x = rs.getInt(1);
            stmt.executeQuery();
            return x;
        } finally {
            if (rs != null)
                rs.close();
        }

    }
    public int foo2(PreparedStatement stmt) throws Exception {
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery();
            rs.next();
            int x = rs.getInt(1);
            rs.close();
            stmt.executeQuery();
            rs = stmt.executeQuery();
            rs.next();
            x += rs.getInt(1);
            return x;
        } finally {
            if (rs != null)
                rs.close();
        }

    }

}
