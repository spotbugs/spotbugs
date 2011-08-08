import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class DoNotRunAThread {

    void f(Runnable r) {
        r.run();
    }

    @ExpectWarning("DMI_THREAD_PASSED_WHERE_RUNNABLE_EXPECTED")
    void g(Thread t) {
        f(t);
    }
    
    @ExpectWarning("RU_INVOKE_RUN")
    void h(Thread t) {
        t.run();
    }

}
