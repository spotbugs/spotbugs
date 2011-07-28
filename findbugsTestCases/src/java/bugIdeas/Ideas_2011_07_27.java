package bugIdeas;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

public class Ideas_2011_07_27 {
    
    HashSet<Integer> bad = new HashSet<Integer>();
    
    public void test(String f) {
        
        try {
            FileInputStream in = new FileInputStream(f);
            int b = in.read();
            System.out.println(b);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (bad.contains(f))
                System.out.println("oops");
        }
    }

}
