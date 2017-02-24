package sfBugs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Bug1609941 {
    boolean b(File f) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(f));
        boolean result = "o".equals(in.readLine());
        in.close();
        return result;
    }

}
