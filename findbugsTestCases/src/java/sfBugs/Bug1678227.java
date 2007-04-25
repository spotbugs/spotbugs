package sfBugs;

import java.util.ArrayList;
import java.util.List;

public class Bug1678227 {
	List<Object> list = new ArrayList<Object>();

	public synchronized void push(Object o) {
		list.add(o);
		notifyAll();
    }

	public synchronized Object pop() throws InterruptedException {
		while (list.isEmpty()) {
			wait();
        }
		return list.remove(list.size() - 1);
	}
}
