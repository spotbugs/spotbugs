package bugIdeas;

import com.google.common.base.Preconditions;

import edu.umd.cs.findbugs.annotations.NoWarning;

import org.jspecify.annotations.NonNull;

public class Ideas_2011_08_05 {

    public String returnNonnull() {
        return "x";
    }
    @NoWarning("RCN")
    public int test1() {
        String x = Preconditions.checkNotNull(returnNonnull());
        return x.hashCode();
    }
    @NoWarning("RCN")
    public int test2(@NonNull String x) {
        Preconditions.checkNotNull(x);
        return x.hashCode();
    }
    @NoWarning("RCN")
    public int test3(String x) {
        int h = x.hashCode();
        Preconditions.checkNotNull(x);
        return h;
    }

    @NoWarning("RCN")
    public int test4() {
        String x = returnNonnull();
        if (x == null)
            throw new RuntimeException();
        return x.hashCode();
    }

    public int test4OK() {
        String x = returnNonnull();
        if (x == null)
            System.out.println("Oops");
        return x.hashCode();
    }

    @NoWarning("RCN")
    public int test5(@NonNull String x) {
        if (x == null)
            throw new RuntimeException();
        return x.hashCode();
    }
    public int test5OK(@NonNull String x) {
        if (x == null)
            System.out.println("Oops");
        return x.hashCode();
    }
    @NoWarning(value="RCN", rank=8)
    public int test6(String x) {
        int h = x.hashCode();
        if (x == null)
            throw new RuntimeException();
        return h;
    }

    public int test6OK(String x) {
        int h = x.hashCode();
        if (x == null)
            System.out.println("Oops");
        return h;
    }
    static class Subclass extends Ideas_2011_08_05 {
        @Override
        @NoWarning("RCN")
        public int test2(String x) {
            Preconditions.checkNotNull(x);
            return x.hashCode();
        }
        @Override
        @NoWarning("RCN")
        public int test5(String x) {
            if (x == null)
                throw new RuntimeException();

            return x.hashCode();
        }
    }

}
