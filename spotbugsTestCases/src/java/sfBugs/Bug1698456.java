package sfBugs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Submitted by: Yvan Norsa Summary:
 * 
 * The FindOpenStream class tries to detect the case where a resource is passed
 * to an external method, for it could be closed there. But if the resource is
 * passed to a method in an array, FindOpenStream does not consider it as
 * potentially closed.
 * 
 * Example : Connection c = null;
 * 
 * try { c = this.openConnection(); } catch (SQLException sqlEx) { // ... }
 * finally { DB.closeConnection(new Connection[]{c}); }
 * 
 * FindBugs (1.2.0-rc4) will indicate an ODR_OPEN_DATABASE_RESOURCE here.
 * 
 * But if you do DB.closeConnection(c); instead, it will be OK.
 * 
 */
public class Bug1698456 {

    public void closeWriters(Writer[] ws) throws IOException {
        for (int i = 0; i < ws.length; i++) {
            ws[i].close();
        }
    }

    public void test() throws IOException {
        final File ff = File.createTempFile("sfq", ".lst");
        PrintWriter mWriter = new PrintWriter(new BufferedWriter(new FileWriter(ff)));
        mWriter.append('h');
        closeWriters(new Writer[] { mWriter });
    }

    public static void main(String args[]) throws IOException {
        Bug1698456 b = new Bug1698456();
        b.test();
    }
}
