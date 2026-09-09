package overridableMethodCall;

import java.io.ObjectInputStream;
import java.util.concurrent.Callable;

public class MethodReferenceReadObjectIndirect3 {
    private int wrapper(Callable f) throws Exception {
        return (int) f.call();
    }

    final int indirectOverridable() throws Exception {
        return wrapper(this::overridableMethod);
    }

    final int indirectPrivate() throws Exception {
        return wrapper(this::privateMethod);
    }

    final int indirectFinal() throws Exception {
        return wrapper(this::finalMethod);
    }

    void readObject(final ObjectInputStream stream) throws Exception {
        wrapper(this::indirectOverridable);
        wrapper(this::indirectPrivate);
        wrapper(this::indirectFinal);
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
