package bugIdeas;

public class Ideas_2008_09_14 {
    public String foo(Object o) {
        if (Integer.class.isInstance(o))
            return (String) o;
        return "";
    }

}
