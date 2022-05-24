import org.apache.avro.reflect.Nullable;

public class UncheckedAvroNullableReturn {
    @Nullable
    String foo() {
        return null;
    }

    void bar() {
        System.out.println(foo().hashCode());
    }
}
