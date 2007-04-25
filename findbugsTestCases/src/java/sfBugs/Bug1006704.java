package sfBugs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Bug1006704 {
	void f(Connection con, Integer key) throws SQLException {
		StringBuffer sql = new StringBuffer("SELECT * FROM xxx  WHERE xxx_id = ?");
		PreparedStatement ps = con.prepareStatement(sql.toString());
        try {
			ps.setInt(1, key.intValue());
			ResultSet rs = ps.executeQuery();
			try {
                rs.next();
				int index = 1;
				Integer firstQuestionId = new Integer(rs.getInt(index++));
				String description = rs.getString(index++);
                Float approvalScore = new Float(rs.getFloat(index++));
			} finally {
				rs.close();
			}
        } finally {
			ps.close();
		}

	}
}
