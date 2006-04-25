import java.util.*;
public class UMAC {
	
	Iterator<Integer> emptyIterator() {
		return new Iterator() {

			public boolean hasNext() {
				return false;
			}

			public boolean hasMoreElements() {
				return false;
			}
			public Object next() {
				throw new NoSuchElementException();
			}

			public void remove() {
				throw new UnsupportedOperationException();
				
			}};
	}

}
