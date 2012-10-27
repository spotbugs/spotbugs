package de.tobject.findbugs;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;

public class AnnotationClasspathInitializer extends ClasspathVariableInitializer {

    private static final String FINDBUGS_ANNOTATIONS = "FINDBUGS_ANNOTATIONS";

    private static final String JSR305_ANNOTATIONS = "JSR305_ANNOTATIONS";

    private static final String FINDBUGS_LIBRARY = "/lib/annotations.jar";

    private static final String JSR305_LIBRARY = "/lib/jsr305.jar";

    @Override
    public void initialize(String variable) {
        Bundle bundle = Platform.getBundle(FindbugsPlugin.PLUGIN_ID);
        if (bundle == null) {
            return;
        }
        String fullPath = getLibraryPath(bundle, FINDBUGS_LIBRARY);
        setVariable(fullPath, FINDBUGS_ANNOTATIONS);
        fullPath = getLibraryPath(bundle, JSR305_LIBRARY);
        setVariable(fullPath, JSR305_ANNOTATIONS);
    }

    private void setVariable(String fullPath, String variableName) {
        if (fullPath == null) {
            FindbugsPlugin.getDefault().logError("unable to find path for variable: " + variableName);
            return;
        }
        try {
            JavaCore.setClasspathVariable(variableName, new Path(fullPath), null);
        } catch (JavaModelException e1) {
            FindbugsPlugin.getDefault().logException(e1, "unable to set annotations classpath");
        }
    }

    private String getLibraryPath(Bundle bundle, String libName) {
        URL installLocation = bundle.getEntry(libName);
        if (installLocation == null) {
            // check if we debugging eclipse and see classpath of the findbugs core project 
        	Bundle bundle2 = Platform.getBundle("findbugs");
            if(bundle2 != null){
                installLocation = bundle2.getEntry(libName);
            }
            
            if(installLocation == null){
                FindbugsPlugin.getDefault().logError("Library not found in plugin: " + libName);
                return null;
            }
        }
        String fullPath = null;
        try {
            URL local = FileLocator.toFileURL(installLocation);
            fullPath = new File(local.getPath()).getCanonicalPath();
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(e, "unable to set classpath for " + libName);
        }
        return fullPath;
    }

}
