package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

import java.util.Arrays;
import java.util.Iterator;

public class Bug1324 implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return Arrays.asList("outer").iterator();
    }

    public static class Super implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return Arrays.asList("super").iterator();
        }
    }
    
    public class Inner extends Super {
        @ExpectWarning("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD")
        public void testOk() {
            String a;
            Iterator<String> it = iterator();
            while(it.hasNext()) {
                a = it.next();
                System.out.println(a);
            }
        }

        /* note that without local variables debug info we cannot distinguish 
         * this method from the previous one, thus warning will still be reported
         */
        @NoWarning("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD")
        public void testFalsePositive() {
            for(String a : this) {
                System.out.println(a);
            }
        }
    }
}
