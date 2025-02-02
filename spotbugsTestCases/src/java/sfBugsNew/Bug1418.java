package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Bug1418 {
    @NoWarning("SIC")
    @SuppressFBWarnings("SIC")
    private final ThreadLocal<Integer> field=new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    @NoWarning("SIC")
    @SuppressFBWarnings("SIC")
    public void test() {
        ThreadLocal<Integer> local=new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return 0;
            }
        };
        System.out.println(local+"/"+field);
    }

    @ExpectWarning("SIC")
    private final ThreadLocal<Integer> field2=new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    @ExpectWarning("SIC")
    public void test2() {
        ThreadLocal<Integer> local=new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return 0;
            }
        };
        System.out.println(local+"/"+field2);
    }
}
