package lambdas;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class Issue552_2 {

    public void wrapSupplier(@Nullable Object wrapped) {
        if (wrapped != null) {
            print(() -> wrapped.toString());
        }
    }

    public void print(@Nullable Supplier<String> supplier) {
        if (supplier != null) {
            System.out.println(supplier.get());
        }
    }
}
