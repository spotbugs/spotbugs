package sfBugs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RFE3189992 {

   public void foo(File f) {
           try {
            FileOutputStream o = new FileOutputStream(f);
            o.write(0);
            o.close();
        } catch (IOException e) {
            IllegalArgumentException e2 = new  IllegalArgumentException("Bad file: " + f);
            e2.initCause(e);
        }



   }

}
