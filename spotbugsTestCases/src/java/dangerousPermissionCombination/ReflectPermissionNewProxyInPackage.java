package dangerousPermissionCombination;

import java.lang.reflect.ReflectPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.SecureClassLoader;

public class ReflectPermissionNewProxyInPackage extends SecureClassLoader {
    @Override
    protected PermissionCollection getPermissions(CodeSource cs) {
        PermissionCollection pc = super.getPermissions(cs);
        pc.add(new ReflectPermission("newProxyInPacakge.*"));   // Permission to create a class loader
        // Other permissions
        return pc;
    }
}
