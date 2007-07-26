import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.*;

public class SqlInjection2 {
	public ServletRequest request;

	public void trueNegative(Statement query) throws SQLException {
		query.executeQuery("select * from " + "ANIMAL");
	}

	private String getName() {
		return "ANIMAL";
	}
	
	public void falsePositive(Statement query) throws SQLException {
		query.executeQuery("select * from " + getName());
	}
	
	public void truePositive(Statement query) throws SQLException {
		query.executeQuery("select * from " + request.getParameter("ANIMAL"));
	}
	
}

