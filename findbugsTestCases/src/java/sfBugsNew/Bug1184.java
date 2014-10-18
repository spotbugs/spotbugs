package sfBugsNew;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1184 {
    @DesireWarning("BX_UNBOXING_IMMEDIATELY_REBOXED") // not generated under Jave 1.8
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        Integer i = System.currentTimeMillis() % 2 == 0 ? 17 : list.get(0);
        System.out.println(i);
    }
    @NoWarning("BX_UNBOXING_IMMEDIATELY_REBOXED")
    public static void main2(String[] args) {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        Integer i = System.currentTimeMillis() % 2 == 0 ? (Integer) 17 : list.get(0);
        System.out.println(i);
    }
}
