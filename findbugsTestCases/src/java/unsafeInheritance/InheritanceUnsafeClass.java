package unsafeInheritance;
import java.io.InputStream;
import java.net.URL;

public class InheritanceUnsafeClass {
	// This detector only reports one per class, so rearrange to check

	public InputStream getResourceOK(String r) {// This should not report a bug
		return InheritanceUnsafeClass.class.getResourceAsStream(r);
	}

	public URL getResource(String r) {
		return getClass().getResource(r);
	}

	public InputStream getResourceBAD(String r) {
		return getClass().getResourceAsStream(r);
	}
}
