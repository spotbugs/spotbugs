package bugPatterns;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SQL_BAD_RESULTSET_ACCESS {
	
	void bug1(ResultSet any) throws SQLException {
		any.getString(0);
	}
	void bug2(ResultSet any) throws SQLException {
		any.getInt(0);
	}
	void bug3(ResultSet any) throws SQLException {
		any.getLong(0);
	}
	void bug4(ResultSet any, int anyInt) throws SQLException {
		any.updateInt(0, anyInt);
	}
	
}
