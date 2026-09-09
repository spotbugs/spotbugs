package ghIssues.issue600;

/**
 * This class is the compiled and decompiled version of TryWithResourcesNested.
 */
public class ExplicitNullCheckNested {
    public void test() throws Exception {
        MyAutocloseable closeable1 = MyAutocloseable.create();

        try {
            MyAutocloseable closeable2 = MyAutocloseable.create();

            try {
                closeable1.exampleMethod();
                closeable2.exampleMethod();
            } catch (Throwable var7) {
                if (closeable2 != null) {
                    try {
                        closeable2.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (closeable2 != null) {
                closeable2.close();
            }
        } catch (Throwable var8) {
            if (closeable1 != null) {
                try {
                    closeable1.close();
                } catch (Throwable var5) {
                    var8.addSuppressed(var5);
                }
            }

            throw var8;
        }

        if (closeable1 != null) {
            closeable1.close();
        }

    }
}
