package sfBugs;

public class Bug2998568 {
    public String str() {
        return null;
    }

    public void test() {
        System.out.println(str().charAt(0));
    }
}
