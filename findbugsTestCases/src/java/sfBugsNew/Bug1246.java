package sfBugsNew;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

import edu.umd.cs.findbugs.annotations.DesireWarning;

public class Bug1246 {

    public static Reader getReader() throws FileNotFoundException {
        return new  FileReader("whatever");
    }
    
    @DesireWarning("NP_DEREFERENCE_OF_READLINE_VALUE")
    public static String withoutLinebreak() throws Exception {
        try (BufferedReader reader = new BufferedReader(getReader())) {
            String sth = checkNotNull(reader.readLine(), "File Empty");
            return sth;
        }
    }
    
    @DesireWarning("NP_DEREFERENCE_OF_READLINE_VALUE")
    public static String withLinebreak() throws Exception {
        try (BufferedReader reader = new BufferedReader(getReader())) {
            String sth = checkNotNull(reader.readLine(), 
                    "File Empty");
            return sth;
        }
    }

    public static <T> T checkNotNull(T object, String message) throws Exception {
        if (object != null) {
            return object;
        }

        throw new Exception(message);
    }
}
