import java.util.*;
import java.net.*;
import java.security.*;

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

	
	
	private static ClassLoader s_classLoader;

	/**
	 * bug 1487961
	 * false positive UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS due to bridge method?
	 * @author Dave Brosius
	 */
    public static void brosius()
    {
        try
        {
            String primahome = System.getProperty("HOME");

            if (primahome != null)
            {
                final URL[] url = new URL[] {new URL("http://localhost/foo")};
                AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
                    {
                        public ClassLoader run()
                        {
                            s_classLoader = new URLClassLoader(url);

                            return s_classLoader;
                        }
                    });
            }
        }
        catch (Exception e)
        {
            s_classLoader = null;
        }
    }


}
