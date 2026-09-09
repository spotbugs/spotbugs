package sfBugsNew;

import java.util.Arrays;
import java.util.Collection;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature318 {
    int add(int x, int y) {
        return x+y;
    }

    String join(Collection c, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for(Object o : c) {
            if(sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(o);
        }
        return sb.toString();
    }

    int[] copyRange(int[] src, int from, int to) {
        int[] result = new int[to-from];
        System.arraycopy(src, from, result, 0, to-from);
        return result;
    }

    static class ABC {
        int a, b, c;

        public ABC(int a, int b) {
            setA(a);
            setB(b);
        }

        public void setA(int a) { this.a = a; }
        public void setB(int b) { this.b = b; }
        public void setC(int c) { this.c = c; }
        public StringBuilder report(StringBuilder sb) { return sb.append(a).append(b).append(c); }
    }

    @NoWarning("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    ABC createABC(int a, int b, int c) {
        ABC abc = new ABC(a, b);
        abc.setC(c);
        return abc;
    }

    @NoWarning("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    ABC createABC2(int a, int b, int c, StringBuilder sb) {
        ABC abc = new ABC(a, b);
        abc.setC(c);
        abc.report(sb);
        return abc;
    }

    @ExpectWarning("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void testAdd() {
        add(3,4);
    }

    @ExpectWarning("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void testJoin() {
        join(Arrays.asList("q", "w", "e"), ",");
    }

    @ExpectWarning("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void testCopy(int[] arr) {
        copyRange(arr, 5, 6);
    }

    @ExpectWarning("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void testNewObject() {
        new ABC(1,2);
    }

    @ExpectWarning("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void testNewObject2() {
        StringBuilder sb = new StringBuilder();
        createABC(1,2,3);
        System.out.println(sb);
    }

    @NoWarning("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void testSideEffect() {
        StringBuilder sb = new StringBuilder();
        createABC2(1, 2, 3, sb);
        System.out.println(sb);
    }
}
