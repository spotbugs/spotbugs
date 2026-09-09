import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class PermissionsSuperNegativeTest extends MyClassLoader {
    PermissionsSuperNegativeTest(URL[] urls) {
        super(urls);
    }

    @NoWarning("PERM")
    protected PermissionCollection getPermissions(CodeSource cs) {
        PermissionCollection pc = super.getPermissions(cs);
        pc.add(new RuntimePermission("exitVM"));
        return pc;
    }
}

class MyClassLoader extends URLClassLoader {
    MyClassLoader(URL[] urls) {
        super(urls);
    }
}
