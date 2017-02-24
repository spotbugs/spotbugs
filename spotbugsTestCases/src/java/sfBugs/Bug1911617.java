package sfBugs;

public class Bug1911617 implements Cloneable {
    public Object perhapsClone(Object o) {
        if (o == null) {
            return "Hello";
        }
        if (o instanceof Bug1911617) {
            return ((Bug1911617) o).clone();
        } else {
            return null;
        }
    }

    @Override
    public Object clone() {
        return null;
    }
}
