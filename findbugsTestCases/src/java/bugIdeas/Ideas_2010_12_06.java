package bugIdeas;

import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListMap;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2010_12_06 {
    
    @NoWarning("NP")
    public static void main(String args[]) {
        
        Comparator<Integer> comparator = (Comparator<Integer>)null;
        ConcurrentSkipListMap<Integer,String> map = new ConcurrentSkipListMap<Integer,String>(comparator);
        for(int i = 0; i < 16; i++)
            map.put(i, Integer.toString(i));
      
        System.out.println(map);
        
    }

}
