package sfBugsNew;

import java.io.*;
import java.sql.*;

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

    @ExpectWarning("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public boolean test(Connection c, String code) throws SQLException {
        return Sql.hasResult(c, "SELECT 1 FROM myTable WHERE code='"+code+"'");
    }

    public static class Sql {
        public static boolean hasResult(Connection c, String query) throws SQLException {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query)) {
                return rs.next();
            }
        }
    }
}
