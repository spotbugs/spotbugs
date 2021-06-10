package ghIssues.issue600;

public class MyAutocloseable implements AutoCloseable {
    @Override
    public void close() throws Exception {
        System.out.println("close");
    }

    public void exampleMethod() {
        System.out.println("exampleMethod");
    }

    public static MyAutocloseable create() {
        return new MyAutocloseable();
    }
}
