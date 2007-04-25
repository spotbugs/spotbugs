
public class DoNotRunAThread {

	void f(Runnable r) {
		r.run();
    }

	void g(Thread t) {
		f(t);
    }

}
