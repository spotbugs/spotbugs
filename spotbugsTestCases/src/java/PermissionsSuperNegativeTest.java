import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;

import edu.umd.cs.findbugs.annotations.NoWarning;

class PermissionsSuperNegativeTest extends URLClassLoader {
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
