package bugIdeas;

public class Ideas_2008_09_15 {
    public String alternativesToInstanceof(Object x) {
        if (Integer.class.isInstance(x))
            return (String) x;
        return "";
    }

    public String alternativesToInstanceofAndCheckedCast(Object x) {
        if (Integer.class.isInstance(x))
            return String.class.cast(x);
        return "";
    }

}
