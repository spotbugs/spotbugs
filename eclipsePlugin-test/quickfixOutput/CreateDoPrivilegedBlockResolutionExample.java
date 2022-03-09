import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class CreateDoPrivilegedBlockResolutionExample {
    public void createClassLoader() {
        AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            public URLClassLoader run() {
                return new URLClassLoader(new URL[] {});
            }
        });
    }
}
