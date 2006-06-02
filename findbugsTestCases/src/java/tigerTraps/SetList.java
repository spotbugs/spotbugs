package tigerTraps;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SetList {
    public static void main(String[] args) {
        Set<Integer>  set =  new LinkedHashSet<Integer>();
        List<Integer> list = new ArrayList<Integer>();
        
        for (int i = -3; i < 3; i++) {
            set.add(i);
            list.add(i);
        }
        for (int i = 0; i < 3; i++) {
            set.remove(i);
            list.remove(i);
        }
        System.out.println(set + " " + list);
    }
}
