package googleBugs;

public class Issue9 {
    private void testNamedInnerClass() {
        class B implements Runnable {
            @Override
            public void run() {
                String s = null;
                s.equals(new Object() {
                    void test() {
                        String s = null;
                        s.equals(new Object() {

                        });
                    }
                });
            }
        }
    }

    private void testAnonymousClass() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String s = null;
                s.equals(new Object() {
                    void test() {
                        String s = null;
                        s.equals(new Object() {

                        });
                    }
                });
            }
        });
    }
}
