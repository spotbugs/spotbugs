package ghIssues.issue600;

public class TryWithResourcesNested {
    public void test() throws Exception {
        try (MyAutocloseable closeable1 = MyAutocloseable.create()) {
            try (MyAutocloseable closeable2 = MyAutocloseable.create()) {
                closeable1.exampleMethod();
                closeable2.exampleMethod();
            }
        }
    }
}
