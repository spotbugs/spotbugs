/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.tobject.findbugs.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.site.PDEState;
import org.eclipse.pde.internal.core.ClasspathUtilCore;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * Helper class to resolve full classpath for Eclipse plugin projects. Plugin projects
 * are very special for Eclipse and they differ from "usual" java projects...
 * @author Andrei
 */
public class PDEClassPathGenerator {


	public static String[] computeClassPath(IJavaProject javaProject){
		String[] classPath = new String[0];
		try {
			// first try to check and resolve plugin project. It can fail if there is no
			// PDE plugins installed in the current Eclipse instance (PDE is optional)
			classPath = createPluginClassPath(javaProject);
		} catch (NoClassDefFoundError ce){
			// ok, we do not have PDE installed, now try to get default java classpath
			classPath = createJavaClasspath(javaProject);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Could not compute aux. classpath for project " + javaProject);
			return classPath;
		}

		if(false){
			System.out.println("classpath: " + classPath.length);
			for (String string : classPath) {
				System.out.println(string);
			}
		}
		return classPath;
	}

	private static String[] createJavaClasspath(IJavaProject javaProject) {
		String[] classPath = new String[0];
		try {
			// doesn't return jre libraries
			classPath = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Could not compute aux. classpath for project " + javaProject);
		}
		return classPath;
	}

	private static String[] createPluginClassPath(IJavaProject javaProject) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel(javaProject.getProject());
		if (model == null || model.getPluginBase().getId() == null){
			return createJavaClasspath(javaProject);
		}

		BundleDescription target = model.getBundleDescription();

		Set<BundleDescription> bundles = new HashSet<BundleDescription>();
		addDependentBundles(target, bundles);

		Set<IClasspathEntry> cpes = new HashSet<IClasspathEntry>();

		for (BundleDescription bd : bundles){
			IPluginModelBase model2 = PluginRegistry.findModel(bd);
			ArrayList<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
			ClasspathUtilCore.addLibraries(model2, classpathEntries);
			for (IClasspathEntry cpe : classpathEntries) {
				if(!cpes.contains(cpe)){
					cpes.add(cpe);
				}
			}
		}

		List<String> pdeClassPath = new ArrayList<String>();
		for (IClasspathEntry cpe: cpes) {
			String location = cpe.getPath().toOSString();
			pdeClassPath.add(location);
		}

		// TODO re-check if this is needed. My paranoia says yes, but my brain says no...
		String[] javaClassPath = createJavaClasspath(javaProject);
		for (String cpe : javaClassPath) {
			if(!pdeClassPath.contains(cpe)){
				pdeClassPath.add(cpe);
			}
		}

		return pdeClassPath.toArray(new String[pdeClassPath.size()]);
	}

	private static void addDependentBundles(BundleDescription bd, Set<BundleDescription> bundles){
		// TODO for some reasons, this does not add "native" fragments for the platform
		// see also: ContributedClasspathEntriesEntry, RequiredPluginsClasspathContainer
		// BundleDescription[] requires = PDEState.getDependentBundles(target);
		BundleDescription[] bundles2 = PDEState.getDependentBundlesWithFragments(bd);
		for (BundleDescription bundleDescription : bundles2) {
			if(!bundles.contains(bundleDescription)){
				bundles.add(bundleDescription);
				addDependentBundles(bundleDescription, bundles);
			}
		}
	}

}
