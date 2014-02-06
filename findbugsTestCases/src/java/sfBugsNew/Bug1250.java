package sfBugsNew;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1250 {
    public static void dumb(OutputStream out) throws Exception { 
        out.write(1);
      }

    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
    public static void bad2() throws Exception {
        OutputStream os = new FileOutputStream(new File("/dev/null")); // Expecting OS here
        os.write(1);
        os.flush();
        os.close();
      }
    
    @DesireWarning("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
      public static void bad3() throws Exception {
        OutputStream os = new FileOutputStream(new File("/dev/null")); // Expecting OS here
        dumb(os);
        os.flush();
        os.close();
      }
}
