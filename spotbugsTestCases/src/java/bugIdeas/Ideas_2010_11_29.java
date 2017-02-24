package bugIdeas;

import java.util.ArrayList;
import java.util.Iterator;

public class Ideas_2010_11_29 {
    /** Concurrent modification bug */
    
    public static void main(String[] args) {
        ArrayList<String> namesList = new ArrayList<String>();

        namesList.add("Kelly");
        namesList.add("John");
        namesList.add("Peter");
        namesList.add("Rose");
        
        
        for (Iterator<String> i = namesList.iterator(); i.hasNext(); ) {
            String name = i.next();
            if (name.equals("Peter")) {
                namesList.remove(name);
            } else {
                System.out.println(name);
            }
        }
    }
}

