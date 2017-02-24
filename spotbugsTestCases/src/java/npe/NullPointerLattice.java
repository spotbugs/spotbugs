package npe;

class NullPointerLattice {

    int f(Object o, boolean b) {
        int r = 42;
        if (b)
            r = o.hashCode();
        else
            o = new Object();
        if (o == null)
            r++;
        return r;
    }
}
