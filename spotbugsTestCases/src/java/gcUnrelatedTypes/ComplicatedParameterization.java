package gcUnrelatedTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class ComplicatedParameterization {

    static class Foo<E> extends HashSet<ArrayList<E>> {
        private static final long serialVersionUID = 1L;
    }

    Foo<Integer> myFoo = new Foo<Integer>();

    void add(ArrayList<Integer> a) {
        myFoo.add(a);
    }

    @NoWarning("GC")
    boolean falsePositive(ArrayList<Integer> a) {
        return myFoo.contains(a);
    }
    
    @NoWarning("GC")
    boolean falsePositive2(List<Integer> a) {
        return myFoo.contains(a);
    }
  
    @DesireWarning("GC")
    boolean bug(ArrayList<Long> a) {
        return myFoo.contains(a);
    }
    @DesireWarning("GC")
    boolean bug(LinkedList<Integer> a) {
        return myFoo.contains(a);
    }


}
