package ghIssues;

public class Issue3978 {

    public void constantIfElseBreak() {
        int x = 1;
        while (true) {
            if (x < 0) {
                System.out.println("Dead path");
            } else {
                break;
            }
        }
    }

    public void constantIfElseContinue() {
        int x = 1;
        while (true) {
            if (x < 0) {
                System.out.println("Dead path");
            } else {
                x = x + 1;
            }
        }
    }
}
