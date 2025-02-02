package ghIssues.issue600;

public class TryWithResourcesMultiple {
    public void test() throws Exception {
        try (MyAutocloseable closeable1 = MyAutocloseable.create(); MyAutocloseable closeable2 = MyAutocloseable.create()) {
            closeable1.exampleMethod();
            closeable2.exampleMethod();
        }
    }
}
