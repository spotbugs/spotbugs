package sfBugsNew;

import java.awt.Point;
import java.util.List;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature333 {
    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo(long a, long b) {
        return ((Long)a).compareTo(b);
    }
    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo(int a, int b) {
        return ((Integer)a).compareTo(b);
    }

    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo(String a, String b) {
        return ((Integer)a.length()).compareTo(b.length());
    }

    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo(Point a, Point b) {
        return ((Integer)a.x).compareTo(b.x);
    }

    @NoWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo(Point a, Integer b) {
        return ((Integer)a.x).compareTo(b);
    }

    @NoWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo(Integer a, Point b) {
        return a.compareTo(b.x);
    }

    @NoWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo(List<Integer> a, int b) {
        return a.get(0).compareTo(b);
    }

    @NoWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo(int a, List<Integer> b) {
        return ((Integer)a).compareTo(b.get(0));
    }

    @NoWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo2(Integer a, int b) {
        return a.compareTo(b);
    }

    @NoWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo3(int a, Integer b) {
        return ((Integer)a).compareTo(b);
    }

    @NoWarning("DM_BOXED_PRIMITIVE_FOR_COMPARE")
    public int compareTo4(Integer a, Integer b) {
        return a.compareTo(b);
    }
}
