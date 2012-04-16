package sfBugs;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.DesireWarning;

public class Bug3483863 {

    interface IInterface1 {
        @Nonnull
        public Object get();
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
        @CheckForNull
        public Object get() {
            return null;
        }

    }

    interface IInterface2 extends IInterface1 {

        @DesireWarning(value = "NP", confidence = Confidence.LOW)
        @CheckForNull
        public Object get();
    }

}
