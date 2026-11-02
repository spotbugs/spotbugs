package ghIssues;

public class Issue4407 {

    public void randomNormal() {
        double rsq, v1, v2;
        do {
            v1 = 2.0 * Math.random() - 1;
            v2 = 2.0 * Math.random() - 1;
            rsq = v1 * v1 + v2 * v2;
        } while (rsq >= 1.0 || rsq == 0.0);
    }
}