import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

class PermissionsSuperTest extends URLClassLoader {
    PermissionsSuperTest(URL[] urls) {
        super(urls);
    }

    @ExpectWarning("PERM")
    protected PermissionCollection getPermissions(CodeSource cs) {
        PermissionCollection pc = new Permissions();
        pc.add(new RuntimePermission("exitVM"));
        return pc;
    }
}
