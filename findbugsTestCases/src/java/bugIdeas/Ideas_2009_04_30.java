package bugIdeas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class Ideas_2009_04_30 {

    public static void main(String args[]) {
        HashMap m = new HashMap();
        System.out.println(m.equals(null));

        LinkedHashMap lhm = (LinkedHashMap) m;

        ArrayList<String> a = new ArrayList<String>();

        String s[] = (String[]) a.toArray();
        Object x = m;
        HashSet set = (HashSet) x;

    }

}
