package ghIssues;
import java.io.Serializable;

public class NullPointerDereferenceInLambda {
    public Thread testLambdaClass() {
    	Thread t = new Thread(() -> {
            String s = null;
            s.equals(new Object() {      // Not report

                void test() {
                    String s = null;
                    s.equals(new Object() {   // report
                    });
                }
            });
        });
        
        return t;
    }
}
