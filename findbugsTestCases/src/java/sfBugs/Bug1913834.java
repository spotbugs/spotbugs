package sfBugs;

public class Bug1913834 {

    public int getBlah() {
        Integer i = 7;
        return i.intValue();
    }

    public int getBlah2() {
        Bug1913834 b = new Bug1913834();
        return b.get7();
    }

    public int get7() {
        return 7;
    }
}
