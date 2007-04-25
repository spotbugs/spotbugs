import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class UMAC {

	Serializable x = new Serializable() {
		private static final long serialVersionUID = 1L;
        Object writeReplace() throws ObjectStreamException { return this; }
		Object readResolve() throws ObjectStreamException { return this; }
	};
	Iterator<Integer> emptyIterator() {
		return new Iterator<Integer>() {

			public boolean hasNext() {
				return false;
			}

			public boolean hasMoreElements() {
				return false;
			}
			public Integer next() {
				throw new NoSuchElementException();
			}

			public void remove() {
				throw new UnsupportedOperationException();

			}};
	}


	public static Map<String,String> getLoggingMap() {
		return new HashMap<String, String>() {
			public String get(String key) {
				String result = super.get(key);
				System.out.println("Map("+key+") = " + result);
				return result;
			}
			@Override
			public String put(String key, String value) {
				String result = super.put(key, value);
				System.out.println("Map.put("+key+", " + value + ") = " + result);
				return result;
			}
		};
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
