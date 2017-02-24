package bugIdeas;

import java.util.AbstractList;
import java.util.HashMap;

public class Ideas_2008_12_02 extends AbstractList<String> {

    HashMap<String, String> map = new HashMap<String, String>();

    HashMap<Object, Object> map2 = new HashMap<Object, Object>();

    public void test1() {
        for (String s : map.keySet())
            if (map2.containsKey(s))
                System.out.println("Found intersection");
    }

    public void test2() {
        for (Object o : map2.keySet())
            if (map.containsKey(o))
                System.out.println("Found intersection");
    }

    @Override
    public int indexOf(Object o) {
        if (map.containsKey(o))
            return 1;
        return -1;
    }

    @Override
    public String get(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 16;
    }
}
