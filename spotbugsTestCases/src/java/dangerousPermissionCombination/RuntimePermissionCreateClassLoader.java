package dangerousPermissionCombination;

import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.SecureClassLoader;

public class RuntimePermissionCreateClassLoader extends SecureClassLoader {
    @Override
    protected PermissionCollection getPermissions(CodeSource cs) {
        PermissionCollection pc = super.getPermissions(cs);
        pc.add(new RuntimePermission("createClassLoader"));   // Permission to create a class loader
        // Other permissions
        return pc;
    }
}
