package ghIssues;

public class Issue4306 {

    public static class Subject extends Thread {
    }

    public static class SubjectWithRun extends Thread {
        @Override
        public void run() {
            System.out.println("running");
        }
    }

    public void test() {
        new Subject().start();
    }

    public void testThreadSubclassWithRun() {
        new SubjectWithRun().start();
    }
}
