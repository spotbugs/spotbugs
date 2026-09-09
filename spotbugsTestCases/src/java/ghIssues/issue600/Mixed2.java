package ghIssues.issue600;

public class Mixed2 {
    public void test() throws Exception {
        try (MyAutocloseable closeable = MyAutocloseable.create()) {
            closeable.exampleMethod();
        }

        MyAutocloseable closeable = MyAutocloseable.create();
        try {
            closeable.exampleMethod();
        } catch (Throwable var5) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Throwable var4) {
                    var5.addSuppressed(var4);
                }
            }
            throw var5;
        }

        if (closeable != null) {
            closeable.close();
        }
    }
}
