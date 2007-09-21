package sfBugs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Bug1356076 {
    PrintWriter[] mWriters = new PrintWriter[1];
    PrintWriter mWriter = null;

    public void writeString1(final String s) throws IOException {
      if (mWriters[0] == null) {
        final File ff = File.createTempFile("sfq", ".lst");
        ff.deleteOnExit();
        mWriter = new PrintWriter(new BufferedWriter(new FileWriter(ff)));
        //mWriters[0] = new PrintWriter(new BufferedWriter(new FileWriter(ff)));
      }
      mWriter.println(s);
      //mWriters[0].println(s);
    }

    // FindBugs fails to notice that this nWriters escapes too
    //// grep -A 1 OS_OPEN_STREAM | grep Bug1356076
    public void writeString2(final String s) throws IOException {
        if (mWriters[0] == null) {
          final File ff = File.createTempFile("sfq", ".lst");
          ff.deleteOnExit();
          //mWriter = new PrintWriter(new BufferedWriter(new FileWriter(ff)));
          mWriters[0] = new PrintWriter(new BufferedWriter(new FileWriter(ff)));
        }
        //mWriter.println(s);
        mWriters[0].println(s);
      }
}
