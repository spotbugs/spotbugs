package lambdas;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class Issue552_1 {

    public void wrapSupplier(@Nullable Supplier<Object> wrapped) {
        if (wrapped != null) {
            print(() -> wrapped.get().toString());
        }
    }

    public void print(@Nullable Supplier<Object> supplier) {
        if (supplier != null) {
            System.out.println(supplier.get());
        }
    }
}
