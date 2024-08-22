package overridableMethodCall;

import java.io.ObjectInputStream;
import java.util.concurrent.Callable;

public class MethodReferenceReadObject {
    private void wrapper(Callable f) throws Exception {
        f.call();
    }

    void readObject(final ObjectInputStream stream) throws Exception {
        wrapper(this::overridableMethod);
        wrapper(this::privateMethod);
        wrapper(this::finalMethod);
    }

    int overridableMethod() {
        return 1;
    }

    private int privateMethod() {
        return 2;
    }

    final int finalMethod() {
        return 3;
    }
}
