package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1397 {
    private static String[] s1;
    @ExpectWarning("MS_PKGPROTECT")
    public static Integer[] s2;
    
    private String[] f1;
    public Integer[] f2;

    @ExpectWarning("EI_EXPOSE_REP2")
    public Bug1397(String[] arr) {
        f1 = arr;
    }

    // The field is public anyways
    @NoWarning("EI_EXPOSE_REP2")
    public Bug1397(Integer[] arr) {
        f2 = arr;
    }
    
    @ExpectWarning("EI_EXPOSE_REP")
    public String[] getF1() {
        return f1;
    }
    
    // The field is public anyways
    @NoWarning("EI_EXPOSE_REP")
    public Integer[] getF2() {
        return f2;
    }
    
    @ExpectWarning("EI_EXPOSE_STATIC_REP2")
    public static void setS1(String[] arr) {
        s1 = arr;
    }

    // The static field is public anyways: the warning is reported on the field itself
    @NoWarning("EI_EXPOSE_STATIC_REP2")
    public static void setS2(Integer[] arr) {
        s2 = arr;
    }

    @ExpectWarning("MS_EXPOSE_REP")
    public static String[] getS1() {
        return s1;
    }
    
    // The field is public anyways: the warning is reported on the field itself
    @NoWarning("MS_EXPOSE_REP")
    public static Integer[] getS2() {
        return s2;
    }
}
