package inefficient;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

class InefficientIndexOfDemo {

    // indexOf(x)

    @NoWarning("IIO")
    int indexOf_1_ok(String s1, String s2) {
       return s1.indexOf(s2);
    }

    @NoWarning("IIO")
    int indexOf_1_ok_int(String s1) {
        return s1.indexOf('.');
    }

    @NoWarning("IIO")
    int indexOf_1_ok_2chars(String s1) {
        return s1.indexOf("xx");
    }

    @ExpectWarning("IIO")
    int indexOf_1_nok(String s1) {
        return s1.indexOf(".");
    }


    // indexOf(x, int)

    @NoWarning("IIO")
    int indexOf_2_ok(String s1, String s2) {
        return s1.indexOf(s2, 0);
    }

    @NoWarning("IIO")
    int indexOf_2_ok_int(String s1) {
        return s1.indexOf('.', 0);
    }

    @NoWarning("IIO")
    int indexOf_2_ok_2chars(String s1) {
        return s1.indexOf("xx", 0);
    }

    @ExpectWarning("IIO")
    int indexOf_2_nok(String s1) {
        return s1.indexOf(".", 0);
    }


    // lastIndexOf(x)

    @NoWarning("IIO")
    int lastIndexOf_1_ok(String s1, String s2) {
        return s1.lastIndexOf(s2);
    }

    @NoWarning("IIO")
    int lastIndexOf_1_ok_int(String s1) {
        return s1.lastIndexOf('.');
    }

    @NoWarning("IIO")
    int lastIndexOf_1_ok_2chars(String s1) {
        return s1.lastIndexOf("xx");
    }

    @ExpectWarning("IIO")
    int lastIndexOf_1_nok(String s1) {
        return s1.lastIndexOf(".");
    }


    // lastIndexOf(x, int)

    @NoWarning("IIO")
    int lastIndexOf_2_ok(String s1, String s2) {
        return s1.lastIndexOf(s2, 0);
    }

    @NoWarning("IIO")
    int lastIndexOf_2_ok_int(String s1) {
        return s1.lastIndexOf('.', 0);
    }

    @NoWarning("IIO")
    int lastIndexOf_2_ok_2chars(String s1) {
        return s1.lastIndexOf("xx", 0);
    }

    @ExpectWarning("IIO")
    int lastIndexOf_2_nok(String s1) {
        return s1.lastIndexOf(".", 0);
    }
}
