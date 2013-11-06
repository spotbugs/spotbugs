package sfBugs;

import java.util.Iterator;
import java.util.List;

public class Bug3330970 {
    public void go(List list) {
        Iterator i = list.iterator();
        while (i.hasNext()) {
            //do something with the stuff in the list
        }
    }
}
