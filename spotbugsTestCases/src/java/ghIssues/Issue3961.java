package ghIssues;

import java.lang.ref.Reference;

public class Issue3961 {

    public static void reproduceRisk() {
        Reference[] refs = new Reference[1];
        refs[0].get();
    }
}
