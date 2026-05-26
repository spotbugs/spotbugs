package ghIssues;

public class Issue3893 {
    private static int f;

    void accessTopLevel(Issue3893 o) {
        f = o.f++;
    }

    class Inner {
        void accessWithShadowing(Issue3893 o) {
            int f = o.f++;
        }

        void accessQualified(Issue3893 o) {
            int f = Issue3893.f++;
        }
    }
}
