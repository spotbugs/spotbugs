package sfBugsNew;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1357 {

    @NoWarning("OBL_UNSATISFIED_OBLIGATION")
     static void readContent(File f) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            System.out.println(br.readLine());
        } finally {
            System.out.println("Gets Called");
            if (br != null)
                br.close();
        }
    }
}
