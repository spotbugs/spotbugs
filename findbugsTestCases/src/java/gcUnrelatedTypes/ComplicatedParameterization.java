package gcUnrelatedTypes;

import java.util.ArrayList;
import java.util.HashSet;

public class ComplicatedParameterization {

    static class Foo<E> extends HashSet<ArrayList<E>> {
    }

    Foo<Integer> myFoo = new Foo<Integer>();

    void add(ArrayList<Integer> a) {
        myFoo.add(a);
    }

    boolean falsePositive(ArrayList<Integer> a) {
        return myFoo.contains(a);
    }

}
