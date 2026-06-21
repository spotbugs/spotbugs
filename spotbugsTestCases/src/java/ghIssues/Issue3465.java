package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

class Issue3465 {
    @ExpectWarning("MS_PKGPROTECT")
    public static final int[] ARRAY = {1, 2, 3, 4, 5};

    public static void showBug() {
        System.out.println(ARRAY[0]);
        ARRAY[0] = 10;
        System.out.println(ARRAY[0]);
    }
}
