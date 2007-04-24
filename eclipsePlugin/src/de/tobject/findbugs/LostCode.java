/* 
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003, Peter Friese
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs;

/**
 * This class contains code I did not want to loose.
 * 
 * @author U402101
 * @version 1.0
 * @since 26.09.2003
 */
public abstract class LostCode {
	//	private IResource getCorrespondingSourceFile(IResource resource) throws JavaModelException, CoreException {
	//		if (resource instanceof IFile) {
	//			IFile file = (IFile) resource;
	//			if (DEBUG) {
	//				System.out.println("File location: " + file.getLocation());
	//			}
	//			
	//			IClassFile classFile = JavaCore.createClassFileFrom(file);
	//			IType type = classFile.getType();
	//			
	//			JavaProjectSourceLocation sourceLocator = new JavaProjectSourceLocation(getJavaProject());
	//			if (DEBUG) {
	//				System.out.println("FQN: " + type.getFullyQualifiedName());
	//				System.out.println("FQN 2: " + type.getFullyQualifiedName('$'));
	//			}
	//			// FIXME: this does not work yet!
	//			Object sourceObject = sourceLocator.findSourceElement(type.getFullyQualifiedName());
	//			if (sourceObject instanceof ICompilationUnit) {
	//				ICompilationUnit cu = (ICompilationUnit) sourceObject;
	//				IResource rescu = cu.getUnderlyingResource();
	//				if (DEBUG) {
	//					System.out.println("Source location: " + rescu.getLocation());
	//				}
	//				return rescu;
	//			}
	//		}
	//		return null;
	//	}
	//	private void showClassPathEntries() throws JavaModelException {
	//		IProject project = getProject();
	//		IJavaProject javaProject = (IJavaProject) JavaCore.create(project);
	//		IPath path = javaProject.getOutputLocation();
	//		if (DEBUG) {
	//			System.out.println("Output path: " + path.toOSString());
	//		}
	//		
	//		IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(true);
	//		for (int i = 0; i < classpathEntries.length; i++) {
	//			IClasspathEntry entry = classpathEntries[i];
	//			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
	//				String srcPath = 
	//					(entry.getPath() != null)
	//						? entry.getPath().toOSString()
	//						: "";
	//				String binPath =
	//					(entry.getOutputLocation() != null)
	//						? entry.getOutputLocation().toOSString()
	//						: "";
	//				if (DEBUG) {						
	//					System.out.println("Output path for folder\"" 
	//						+ srcPath 
	//						+ "\" = "  + binPath);
	//				}
	//			}
	//		}
	//	}
	//	public IJavaProject getJavaProject() {
	//		IProject project = getProject();
	//		return (IJavaProject) JavaCore.create(project);
	//	}
	//	private boolean isJavaArtifact(IResource resource) {
	//		if (resource != null) {
	//			if ( (resource.getName().endsWith(".java")) || (resource.getName().endsWith(".class")) ) {
	//				return true;
	//			}
	//		}
	//		return false;
	//	}
	//	private boolean isClassFile(IResource resource) {
	//		if (resource != null) {
	//			if (resource.getName().endsWith(".class")) {
	//				return true;
	//			}
	//		}
	//		return false;
	//	}
	//	private boolean isJavaFile(IResource resource) {
	//		if (resource != null) {
	//			if (resource.getName().endsWith(".java")) {
	//				return true;
	//			}
	//		}
	//		return false;
	//	}
}
