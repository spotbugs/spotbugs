package sourceInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class Test  implements Serializable {
    
    
    /** This class is for testing the sourceInfo capabilities of FindBugs 
     * @throws FileNotFoundException */
    
    
    public Test(File f) throws FileNotFoundException {
        is = new FileInputStream(f);
    }
    private InputStream is;
    
    
    public int infiniteLoop() {
        return infiniteLoop();
    }
    
    public int read() throws IOException {
        return is.read();
    }
    
    public void close() {
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean equals(Object o) {
        return this == o;
    }

}
