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

    public void doubleForLoop() {
        for (double d = -10.0; d < 100.0; ++d) {
            System.out.println(d);
        }
    }

    public void integerWhileLoop() {
        int d = 0;
        while (d < 100.0) {
            System.out.println(d);
            d += 10.0;
        }
    }
}
