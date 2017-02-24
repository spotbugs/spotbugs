package sfBugs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class Bug2118411 {

    public static String foo(File file) {

        Scanner s = null;
        try {
            s = new Scanner(new BufferedReader(new FileReader(file)));
            return s.next();
        } catch (IOException e) {
            return null;
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }
}
