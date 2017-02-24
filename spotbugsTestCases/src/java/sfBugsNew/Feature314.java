package sfBugsNew;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature314 {
    @ExpectWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void testHardCoded() throws FileNotFoundException {
        openFile("c:\\file.txt");
    }
    
    @ExpectWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void testHardCodedSafe() throws FileNotFoundException {
        openFileSafe("c:\\file.txt");
    }
    
    @ExpectWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void testHardCodedPuzzling() throws FileNotFoundException {
        openFilePuzzling4("ok", "c:\\file.txt", "ok", "ok");
    }
    
    @NoWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void testHardCodedPuzzlingOk() throws FileNotFoundException {
        openFilePuzzling4("c:\\file.txt", "ok", "c:\\file.txt", "c:\\file.txt");
    }
    
    @ExpectWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void testHardCodedLong() throws FileNotFoundException {
        openFilePuzzlingLong(1L, "c:\\file.txt", "ok", 0.0);
    }
    
    @NoWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void testHardCodedLongOk() throws FileNotFoundException {
        openFilePuzzlingLong(1L, "ok", "c:\\file.txt", 0.0);
    }
    
    private FileOutputStream openFile(String name) throws FileNotFoundException {
        return new FileOutputStream(name);
    }

    private FileOutputStream openFileSafe(String name) throws FileNotFoundException {
        if(name == null) return null;
        return openFile(name);
    }

    private FileOutputStream openFilePuzzling(String arg1, String arg2, String name, String arg3) throws FileNotFoundException {
        return openFileSafe(name);
    }

    private static FileOutputStream openFilePuzzling2(String arg1, String arg3, String arg2, String name) throws FileNotFoundException {
        return new Feature314().openFilePuzzling(arg1, arg2, name, arg3);
    }

    private FileOutputStream openFilePuzzling3(String name, String arg3, String arg2, String arg1) throws FileNotFoundException {
        return openFilePuzzling2(arg1, arg2, arg3, name);
    }

    private FileOutputStream openFilePuzzling4(String arg1, String name, String arg2, String arg3) throws FileNotFoundException {
        return openFilePuzzling3(name, arg1, arg2, arg3);
    }

    private FileOutputStream openFilePuzzlingLong(long arg1, String name, String arg2, double arg3) throws FileNotFoundException {
        return openFilePuzzling3(name, String.valueOf(arg1), arg2, String.valueOf(arg3));
    }
    
    @ExpectWarning("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public boolean test(Connection c, String code) throws SQLException {
        return Sql.hasResult(c, "SELECT 1 FROM myTable WHERE code='"+code+"'");
    }

    @ExpectWarning("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public boolean testSqlLong(Connection c, String code) throws SQLException {
        return Sql.hasResult(c, 1L, "SELECT 1 FROM myTable WHERE code='"+code+"'", 2L, "blahblah");
    }
    
    @NoWarning("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public boolean testSqlLongOk(Connection c, String code) throws SQLException {
        return Sql.hasResult(c, 1L, "SELECT COUNT(*) FROM myTable", 2L, "Code: "+code);
    }
    
    public static class Sql {
        // passthru method: warning is generated on call site
        @NoWarning("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
        public static boolean hasResult(Connection c, String query) throws SQLException {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query)) {
                return rs.next();
            }
        }

        // passthru method: warning is generated on call site
        @NoWarning("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
        public static boolean hasResult(Connection c, long l1, String query, long l2, String somethingElse) throws SQLException {
            System.out.println(somethingElse+": "+l1+":"+l2);
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query)) {
                return rs.next();
            }
        }
    }
}
