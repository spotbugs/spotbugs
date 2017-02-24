import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

class DumbMethodInvocations implements Iterator {
    @ExpectWarning("DMI_EMPTY_DB_PASSWORD")
    Connection getConnection1() throws SQLException {
        return DriverManager.getConnection("jdbc:hsqldb:mem:test", "sa", "");
    }

    @ExpectWarning("DMI_CONSTANT_DB_PASSWORD")
    Connection getConnection2() throws SQLException {
        return DriverManager.getConnection("jdbc:hsqldb:mem:test", "sa", "secret");
    }

    @ExpectWarning("DMI_USELESS_SUBSTRING")
    String f(String s) {
        return s.substring(0);
    }

    String g(String s) {
        for (int i = 0; i < s.length(); i++)
            if (s.substring(i).hashCode() == 42)
                return s;
        return null;
    }

    @Override
    @ExpectWarning("DMI_CALLING_NEXT_FROM_HASNEXT")
    public boolean hasNext() {
        return next() != null;
    }

    @Override
    @ExpectWarning("IT_NO_SUCH_ELEMENT")
    public Object next() {
        return null;
    }

    @Override
    public void remove() {
    }

    public void falsePositive() {
        Date today = Calendar.getInstance().getTime();
        System.out.println(today);
        today.setDate(16);
        System.out.println(today);
    }

    @ExpectWarning("DMI_LONG_BITS_TO_DOUBLE_INVOKED_ON_INT")
    double convertToDouble(int i) {
        return Double.longBitsToDouble(i);
    }

    @ExpectWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void testFile() {
        new File("c:\\test.txt");
    }

    @ExpectWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void testFile2() {
        new File("c:\\temp", "test.txt");
    }

    @ExpectWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void testPrintStream() throws IOException {
        new PrintStream("c:\\test.txt", "UTF-8");
    }

    @NoWarning("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void testPrintStream2() throws IOException {
        new PrintStream("UTF-8", "c:\\test.txt");
    }
}
