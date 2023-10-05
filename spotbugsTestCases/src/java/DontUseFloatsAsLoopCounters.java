import java.util.Random;

class DontUseFloatsAsLoopCounters {
    // Noncompliant

    public static void test1() {
        float x = 0.1f;
        while (x<10){
            System.out.println(x);
            x++;
        }
    }

    public static void test2() {
        for (float y = 0.2f; y <= 1.0f; y += 0.1f) {
            System.out.println(y);
        }
    }

    public static void test3() {
        for (double d = 0.2d; d <= 1.0d; d += 0.1d) {
            System.out.println(d);
        }
    }

    // Compliant
    public static void test4() {
        for (int count = 1; count <= 10; count += 1) {
            float q = count/10.0f;
            System.out.println(q);
            System.out.println(count);
        }
    }

    public static void test5() {
        int c = 0;
        while (c<5){
            c++;
            System.out.println(c);
        }
    }

    public static void test6() {
        boolean b = true;
        while (b){
            b = false;
            System.out.println(b);
        }
    }

    public static void test7() {
        int p = 1;
        while (p<9){
            p*=2;
            System.out.println(p);
        }
    }

    private static Random rnd = new Random();

    public static double test8() {
        double total = 0;

        for (int attempt = 1;; attempt++) {
            total += rnd.nextDouble();

            if (rnd.nextDouble() < 0.5) {
                System.out.println(attempt);
                break;
            }
        }

        return total;
    }

    public static void test9(boolean someOtherCondition) {
        double y = Double.NaN;
        while (Double.isNaN(y) && someOtherCondition) {
            System.out.println("y before" + y);
            y = Double.parseDouble(System.console().readLine());
            System.out.println("y after" + y);
        }
    }

    public static double test10(double t) {
        double kmin = 0, kmax = 1;
        while (Math.log(1 + 2 * kmax) - 2 * Math.log(1 + kmax) < t) {
            kmin = kmax;
            kmax *= 2;
        }

        double k = (kmin + kmax) / 2;
        while (kmin < k && k < kmax) {
            if (Math.log(1 + 2 * k) - 2 * Math.log(1 + k) < t) {
                kmin = k;
            } else {
                kmax = k;
            }
            k = (kmin + kmax) / 2;
        }
        return k;
    }

    //compliant
    //false positive sample provided in issue #2071
    public static int ilog10(double x) {
        if (x >= 1.0) {
            int i = 0;
            while ((x /= 10.0) >= 1.0) { // <-- HERE
                i++;
            }
            return i;
        } else {
            int i = -1;
            while ((x *= 10.0) < 1.0) { // <-- HERE
                i--;
            }
            return i;
        }
    }
}
