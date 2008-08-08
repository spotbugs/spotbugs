package bugIdeas;

import java.util.HashSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Ideas_2008_08_06<E> extends HashSet<E> {

	public int bar() {
		throw new UnsupportedOperationException();
	}

	public int foo() {
		return bar();
	}

	public static void main(String args[]) {
		Ideas_2008_08_06<String> i = new Ideas_2008_08_06<String>();
		System.out.println(i.contains((Integer) 5));
		ScheduledThreadPoolExecutor e = new ScheduledThreadPoolExecutor(0);
		e.setMaximumPoolSize(10);
	}

}
