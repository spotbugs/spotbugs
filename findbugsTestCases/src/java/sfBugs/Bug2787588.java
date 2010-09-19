package sfBugs;

import java.util.HashMap;
import java.util.Map;

public class Bug2787588 {

    public static void main(String[] st2) {

        Map<String, Long> xMap = new HashMap<String, Long>();
        Long count = Long.valueOf(0);
        xMap.put("Value", ++count);
        System.out.println("Map:" + xMap.toString());
    }
}
