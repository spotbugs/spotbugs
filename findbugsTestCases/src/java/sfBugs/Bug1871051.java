package sfBugs;

public class Bug1871051 {
    @Override
    public Object clone() {
        return new Bug1871051();
    }
}
