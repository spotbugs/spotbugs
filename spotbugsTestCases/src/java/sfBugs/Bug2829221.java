package sfBugs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Bug2829221 {
    public static void test(String[] args) throws IOException {
        InputStream is = new FileInputStream("test.txt");
        OutputStream os = new FileOutputStream("/tmp/test.txt");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            PrintWriter w = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                w.println(line);
            }
        } finally {
            is.close();
        }
    }

    public static void main(String[] args) throws IOException {
        InputStream is = new FileInputStream("test.txt");
        OutputStream os = new FileOutputStream("/tmp/test.txt");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            PrintWriter w = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                w.println(line);
            }
        } finally {
            is.close();
        }
    }
}
