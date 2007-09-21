package sfBugs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * 
 *
 */
public class Bug1698456 {

    public void closeWriters(Writer[] ws) throws IOException {
        for(int i = 0; i < ws.length; i++) {
            ws[i].close();
        }
    }

    public void test() throws IOException {
        final File ff = File.createTempFile("sfq", ".lst");
        PrintWriter mWriter = new PrintWriter(new BufferedWriter(new FileWriter(ff)));
        mWriter.append('h');
        closeWriters(new Writer[] {mWriter});
    }
    
    public static void main(String args[]) throws IOException {
        Bug1698456 b = new Bug1698456();
        b.test();
    }
}
