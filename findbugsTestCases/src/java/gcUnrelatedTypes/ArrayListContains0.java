package gcUnrelatedTypes;

import java.util.ArrayList;

public class ArrayListContains0<T> {

    static class Dummy {

    }

    static class DummyChild extends Dummy {

    }

    private ArrayList<?> wildcardF;

    private ArrayList<Dummy> dummyF;

    private ArrayList<? extends Dummy> dummyEF;

    private ArrayList<? super Dummy> dummySF;

    private ArrayList<DummyChild> childF;

    private ArrayList<? extends DummyChild> childEF;

    private ArrayList<? super DummyChild> childSF;

    private ArrayList<T> genericF;

    private ArrayList<? extends T> genericEF;

    private ArrayList<? super T> genericSF;

    public ArrayListContains0(ArrayList<?> wildcardF, ArrayList<Dummy> dummyF, ArrayList<? extends Dummy> dummyEF,
            ArrayList<? super Dummy> dummySF, ArrayList<DummyChild> childF, ArrayList<? extends DummyChild> childEF,
            ArrayList<? super DummyChild> childSF, ArrayList<T> genericF, ArrayList<? extends T> genericEF,
            ArrayList<? super T> genericSF) {

        Dummy dummy = new Dummy();
        DummyChild dummyChild = new DummyChild();
        String s = "Mismatched Type";

        wildcardF.contains(s); // No warning

        dummyF.contains(s); // HIGH

        dummyEF.contains(s); // HIGH

        dummySF.contains(s); // HIGH

        childF.contains(s); // HIGH

        childEF.contains(s); // HIGH

        childSF.contains(s); // HIGH

    }

}
