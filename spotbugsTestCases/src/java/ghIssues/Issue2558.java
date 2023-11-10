package ghIssues;

public class Issue2558 {

    public boolean equals(@edu.umd.cs.findbugs.annotations.NonNull Object a) { //report a warning, but it is an FP
        return a.hashCode() == this.hashCode();
    }
    public boolean something(Issue2558 a, Issue2558 b) {
        return a.equals(b);
    }
}
