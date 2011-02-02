package sfBugs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3167096 {
    private void method() {
        Properties pro = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("/myFile.properties");
            pro.load(fis);
        }
        catch (FileNotFoundException e) {

            System.out.println(e);
        }
        catch (IOException e) {

            System.out.println(e);
        }
        catch (Exception e) {

            System.out.println(e);
        }
        finally {

            try {
                fis.close();
            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }

            System.out.println("FILEINPUTSTREAM_CLOSED_SUCCESSFULLY");
        }
    }
}
