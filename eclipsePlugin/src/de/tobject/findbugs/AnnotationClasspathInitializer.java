package de.tobject.findbugs;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;

public class AnnotationClasspathInitializer extends
		ClasspathVariableInitializer {

	public final String FINDBUGS_ANNOTATIONS = "FINDBUGS_ANNOTATIONS";

	public final String LIBRARY = "/annotations.jar";

	@Override
	public void initialize(String variable) {
		Bundle bundle = Platform.getBundle("de.tobject.findbugs"); //$NON-NLS-1$
		boolean setVariable = false;
		if (bundle != null)
			try {
				URL installLocation = bundle.getEntry(LIBRARY); //$NON-NLS-1$
				URL local = Platform.asLocalURL(installLocation);

				String fullPath = new File(local.getPath()).getAbsolutePath();
				JavaCore.setClasspathVariable(FINDBUGS_ANNOTATIONS, 
						new Path(fullPath), null);
				setVariable = true;
			} catch (JavaModelException e1) {
				// will clear variable below

			} catch (IOException e) {
				// will clear variable below
			}

		if (!setVariable)
			JavaCore.removeClasspathVariable(FINDBUGS_ANNOTATIONS, null);

	}

}
