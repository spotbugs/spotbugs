package ghIssues;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Issue3990 {

    private static final String STATIC_PASS = "static_cred";

    public void directHardcodedPassword() throws SQLException {
        DriverManager.getConnection("jdbc:mysql://localhost", "user", "direct_hardcoded");
    }

    public void propertiesPassword() throws SQLException {
        Properties props = new Properties();
        props.setProperty("password", "prop_cred");
        DriverManager.getConnection("jdbc:mysql://localhost", props);
    }

    public void urlEmbeddedPassword() throws SQLException {
        DriverManager.getConnection("jdbc:mysql://localhost/db?user=admin&password=url_sec");
    }

    public void localVariablePassword() throws SQLException {
        String dynamicPass = STATIC_PASS;
        DriverManager.getConnection("jdbc:mysql://localhost", "admin", dynamicPass);
    }

    public void emptyPassword() throws SQLException {
        String emptyPass = "";
        DriverManager.getConnection("jdbc:mysql://localhost", "root", emptyPass);
    }
}
