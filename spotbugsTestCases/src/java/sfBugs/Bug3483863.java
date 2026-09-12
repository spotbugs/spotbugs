package sfBugs;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.DesireWarning;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Bug3483863 {

    interface IInterface1 {
        public @NonNull Object get();
    }

    class CClass2 {

        @DesireNoWarning(value = "NP", confidence = Confidence.LOW)
        public void test(IInterface1 x) {
            Object a = x.get();
            System.out.println(a.toString());
        }
    }

    class CClass implements IInterface1 {

        @DesireWarning(value = "NP", confidence = Confidence.LOW)
        @Override
        public @Nullable Object get() {
            return null;
        }

    }

    interface IInterface2 extends IInterface1 {

        @Override
        @DesireWarning(value = "NP", confidence = Confidence.LOW)
        public @Nullable Object get();
    }

}
