package sfBugs;

public class Bug1941804 {
    private static String LOCK = "LOCK";

    private String nsLOCK = "LOCK";

    private final static String fLOCK = "LOCK";

    private final String fnsLOCK = "LOCK";

    public Bug1941804() {
        super();
    }

    public void methodSynchronize(String parmLOCK) {
        String localLOCK = "LOCK";
        synchronized (LOCK) {
            int a = 1;
            int b = 2;
            int c = a + b;
            System.out.println("c: " + c);
        }
        synchronized (nsLOCK) {
            int a = 1;
            int b = 2;
            int c = a + b;
            System.out.println("c: " + c);
        }
        synchronized (fLOCK) { // warning
            int a = 1;
            int b = 2;
            int c = a + b;
            System.out.println("c: " + c);
        }
        synchronized (fnsLOCK) { // warning
            int a = 1;
            int b = 2;
            int c = a + b;
            System.out.println("c: " + c);
        }
        synchronized (parmLOCK) {
            // Code...
            int a = 1;
            int b = 2;
            int c = a + b;
            System.out.println("c: " + c);
        }
        synchronized (localLOCK) { // warning
            // Code...
            int a = 1;
            int b = 2;
            int c = a + b;
            System.out.println("c: " + c);
        }
    }
}
