package sfBugs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Bug1923609 {
	
	// Attempt to get FindBugs to warn on writing a HashMap to an ObjectOutputStream
	
	public void writeMapStringString() throws IOException {
		ObjectOutputStream os = new ObjectOutputStream(new ByteArrayOutputStream());
		Map<String,String> m = new HashMap<String,String>();
		m.put("red", "blue");
		os.writeObject(m);
	}

	// Another attempt to get FindBugs to warn on writing a HashMap to an
	// ObjectOutputStream, this time using a map parameterized no (non-
	// serializable) Threads.
	
	static class DummyThread extends Thread {
		public void run() {
			System.out.println("Dummy");
		}
	}
	
	public void writeMapThreadThread() throws IOException {
		ObjectOutputStream os = new ObjectOutputStream(new ByteArrayOutputStream());
		Map<Thread,Thread> m = new HashMap<Thread,Thread>();
		m.put(new DummyThread(), new DummyThread());
		os.writeObject(m);
	}

	// Final attempt: contrive a class that implements Map but isn't
	// serializable and try to write it to an ObjectOutputStream.  This does
	// generate a warning
	
	
	static class MyMap<T1,T2> implements Map<T1,T2> {
		
		private Thread dummy; // makes MyMap unserializable
		public void setThread(Thread t)  {
			dummy = t;
		}
		public Thread getThread() {
			return dummy;
		}

		public void clear() {
	        // TODO Auto-generated method stub
	        
        }

		public boolean containsKey(Object key) {
	        // TODO Auto-generated method stub
	        return false;
        }

		public boolean containsValue(Object value) {
	        // TODO Auto-generated method stub
	        return false;
        }

		public Set<java.util.Map.Entry<T1, T2>> entrySet() {
	        // TODO Auto-generated method stub
	        return null;
        }

		public T2 get(Object key) {
	        // TODO Auto-generated method stub
	        return null;
        }

		public boolean isEmpty() {
	        // TODO Auto-generated method stub
	        return false;
        }

		public Set<T1> keySet() {
	        // TODO Auto-generated method stub
	        return null;
        }

		public T2 put(T1 key, T2 value) {
	        // TODO Auto-generated method stub
	        return null;
        }

		public void putAll(Map<? extends T1, ? extends T2> t) {
	        // TODO Auto-generated method stub
	        
        }

		public T2 remove(Object key) {
	        // TODO Auto-generated method stub
	        return null;
        }

		public int size() {
	        // TODO Auto-generated method stub
	        return 0;
        }

		public Collection<T2> values() {
	        // TODO Auto-generated method stub
	        return null;
        }
		
	}

	public void writeMyMap() throws IOException {
 		ObjectOutputStream os = new ObjectOutputStream(new ByteArrayOutputStream());
		Map<String,String> m = new MyMap<String,String>();
		m.put("red", "blue");
		os.writeObject(m);
	}
}
