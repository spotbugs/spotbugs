import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletRequest;

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

    public void trueHigherPriorityPositive(Statement query) throws SQLException {
        query.executeQuery("select * from zoon where animal='" + request.getParameter("ANIMAL") + "'");
    }

}
