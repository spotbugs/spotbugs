package sfBugs;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.WillClose;
import javax.servlet.http.HttpServletRequest;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class Bug3600398 {

    abstract Connection getConnection();

    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    public void subscribe(HttpServletRequest req) throws Exception {
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            connection = getConnection();
            stmt = connection.prepareStatement("...");
            stmt.setString(1, "a");
            stmt.executeUpdate();
        } finally {
            close(stmt, connection);
        }
    }
    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
    public void subscribe2(HttpServletRequest req) throws Exception {
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            connection = getConnection();
            stmt = connection.prepareStatement("...");
            stmt.setString(1, "a");
            stmt.executeUpdate();
        } finally {
            close("a", stmt, 0.0, connection, 0);
        }
    }
    @DesireNoWarning("OBL_UNSATISFIED_OBLIGATION")
    public void subscribe3(HttpServletRequest req) throws Exception {
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            connection = getConnection();
            stmt = connection.prepareStatement("...");
            stmt.setString(1, "a");
            stmt.executeUpdate();
        } finally {
            close(stmt, connection, null, null);
        }
    }

    void close(@WillClose Object object1, @WillClose Object object2) {
        close(object1); close(object2);
    }
    void close(String x, @WillClose Object object1, double y, @WillClose Object object2, int z) {
        close(object1); close(object2);
    }
    void close(@WillClose Object object1, @WillClose Object object2, @WillClose Object object3) {
        close(object1); close(object2); close(object3);
    }
    void close(@WillClose Object object) {
        if (object != null) {
            if (object instanceof Closeable) {
                try { ((Closeable)object).close(); } catch (IOException ignore) { }
            } else if (object instanceof Statement) {
                try { ((Statement)object).close(); } catch (SQLException ignore) { }
            } else if (object instanceof Connection) {
                try { ((Connection)object).close(); } catch (SQLException ignore) { }
            } else if (object instanceof ResultSet) {
                try { ((ResultSet)object).close(); } catch (SQLException ignore) { }
//                    } else if (object instanceof JSONWriter) {
//                        try { ((JSONWriter)object).close(); } catch (Exception ignore) { }
            } else {
                throw new IllegalArgumentException("Cannot handle class:" + object.getClass().getName());
            }
        }
    }
    
    @WillClose
    protected void close(Object... objects) {
        if (objects != null) {
            for (Object object : objects) {
                close(object);
            }
        }
    }


}
