package bugIdeas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Ideas_2010_12_09 {

    protected <T> T throwException( String msg) {
        throw new RuntimeException(msg);
    }
    
    public int hash(Object o) {
        if (o == null)
            throwException("Oops");
        return o.hashCode();
    }
    
    static class MultivaluedMap<K,V> extends HashMap<K,List<V>> {
        
        V foo() {
            return null;
        }
        
    }
    static MultivaluedMap<String,String> m = new MultivaluedMap<String, String>();
    
    public static void main(String args[] ) {
       
        List<String> lst = new ArrayList<String>();
        lst.add("a");
        lst.add("b");
        m.put("x", lst);
        MultivaluedMap<String,String> m2 = m;
        for(String s : m.get("x"))
            System.out.println(s);
        for(String s : m2.get("x"))
            System.out.println(s);
        
    }

}
