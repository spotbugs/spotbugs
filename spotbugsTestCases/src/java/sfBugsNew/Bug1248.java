package sfBugsNew;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1248 {
    @Nonnull
    private final List<Integer> foo = Collections.emptyList();

    public void test0() {
        if (foo != null) { // warning, good
            System.out.println("non null");
        }
        if (foo == null) { // no warning, bad
            System.out.println("null");
        }
    }

//    @ExpectWarning(value = "RCN", num = 1)
//    @NoWarning(value = "RCN", num = 2)
//    public void test() {
//        if (foo != null) { // warning, good
//            throw new IllegalStateException();
//        }
//        if (foo == null) { // no warning, bad
//            throw new IllegalStateException();
//        }
//    }
//
//    @ExpectWarning("RCN")
//    public void test2() {
//        if (foo != null) { // warning, good
//            throw new IllegalStateException();
//        }
//
//    }
//
//    @NoWarning("RCN")
//    public void test3() {
//        if (foo == null) { // no warning, bad
//            throw new IllegalStateException();
//        }
//    }
//
//    @ExpectWarning("RCN")
//    public void test4() {
//        if (foo != null) { // warning, good
//            System.out.println("non null");
//        }
//
//    }
//
//    @ExpectWarning("RCN")
//    public void test5() {
//        if (foo == null) { // no warning, bad
//            System.out.println("null");
//        }
//    }
}
