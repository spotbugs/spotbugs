package ghIssues;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Issue79 {
    private static final String QUERY = "";

    public void f(Connection cnx) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = cnx.prepareStatement(QUERY);
            rs = st.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString("ID"));
            }
        } finally {
            /*
             * The statement closes the result set, and there is no scenario where st may be null
             * but not the resultset, however an unsatisfied obligation is reported on the resultset
             */
            if (st != null) {
                st.close();
            }
        }
    }
}
