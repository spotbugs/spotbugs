package ghIssues;

import org.jspecify.annotations.NonNull;

public class Issue2558 {

    public boolean equals(@NonNull Object a) { // False positive solved by the change
        return a.hashCode() == this.hashCode();
    }
    public boolean something(Issue2558 a, Issue2558 b) {
        return a.equals(b);
    }
}
