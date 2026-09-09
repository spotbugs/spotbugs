import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Issue2782 {

    public interface MyInterface<T> {
    }

    static class Impl1 implements MyInterface<String> {
        public MyInterface<String> helloFromImpl1() {
            return this;
        }
    }

    static class Impl2 implements MyInterface<Boolean> {
        public MyInterface<Boolean> helloFromImpl2() {
            return this;
        }
    }

    public static <T> void method(MyInterface<T> instance) {
        Object bla = switch (instance) {
            case Impl1 impl1 -> impl1.helloFromImpl1();
            case Impl2 impl2 -> impl2.helloFromImpl2();
            default -> throw new IllegalArgumentException("Blah");
        };

        System.out.println(bla);
    }

    public static <T> Object nestedSwitches(MyInterface<T> x1, MyInterface<T> x2, MyInterface<T> x3) {
        return switch (x1) {
            case Impl1 impl1 -> switch (x2) {
                case Impl1 a -> switch (x3) {
                    case Impl1 x -> x.helloFromImpl1();
                    case Impl2 y -> y.helloFromImpl2();
                    default -> "";
                };
                case Impl2 b -> b.helloFromImpl2();
                default -> "";
            };
            case Impl2 impl2 -> impl2.helloFromImpl2();
            default -> throw new IllegalArgumentException("Blah");
        };
    }

    public static int badCast(Object value) {
        return switch (value) {
            case Map map -> ((Set) map.values()).size(); // BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
            case Number n -> ((Integer) n).intValue(); // BC_UNCONFIRMED_CAST
            default -> 42;
        };
    }

    public static int deadStore(int value) {
        switch (value) {
            case 0:
            	int x = 0;
                --x; // DLS_DEAD_LOCAL_STORE
            	return value;
            default:
            	return 42;
        }
    }
    
    public static int collectionSize(Object value) {
        return switch (value) {
            case Map<?, ?> map -> map.size();
            case Set<?> set -> set.size();
            case List<?> list -> list.size();
            case Collection<?> collection ->
            throw new UnsupportedOperationException("Error " + value.getClass());
            case null, default -> 0;
        };
    }
    
    public static String getValueType(Object value) {
        return switch (value) {
            case List<?> list -> "list";
            case Map<?, ?> map -> "map";
            case Number number -> "number";
            case null, default -> "other";
        };
    }
}
