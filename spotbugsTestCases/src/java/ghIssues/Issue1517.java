package ghIssues;

public class Issue1517 {

    public void incrementInAdditionRight() {
        int i = 0;
        i = 2 + (i++);
    }

    public void incrementInAdditionLeft() {
        int i = 0;
        i = (i++) + 2;
    }

    public void decrementInSubtractionLeft() {
        int i = 0;
        i = (i--) - 2;
    }

    public void decrementInAdditionRight() {
        int i = 0;
        i = 2 + (i--);
    }

    public void directSelfAssignment() {
        int i = 0;
        i = i++;
    }

    public void standaloneIncrementThenAssign() {
        int i = 0;
        i++;
        i = 2 + i;
    }
}
