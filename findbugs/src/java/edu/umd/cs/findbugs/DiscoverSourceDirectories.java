/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2008 David H. Hovemeyer <david.hovemeyer@gmail.com>
 * Copyright (C) 2008 University of Maryland
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

package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.ba.ClassNotFoundExceptionParser;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.IClassPathBuilderProgress;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.engine.ClassParserUsingASM;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;

/**
 * Based on the contents of the application
 * directories/archives in a Project,
 * and a "root" source directory (under which some
 * number of "real" source directories may be located),
 * scan to find the source directories containing
 * the application's source files.
 * 
 * @author David Hovemeyer
 */
public class DiscoverSourceDirectories {
	private static boolean DEBUG = SystemProperties.getBoolean("findbugs.dsd.debug");

	/**
	 * Progress callback interface for reporting the
	 * progress of source directory discovery.
	 */
	public interface Progress extends IClassPathBuilderProgress {
		public void startRecursiveDirectorySearch();
		public void doneRecursiveDirectorySearch();
		
		public void startScanningArchives(int numArchivesToScan);
		public void doneScanningArchives();
		
		public void startScanningClasses(int numClassesToScan);
		public void finishClass();
		public void doneScanningClasses();
	}

	private static class NoOpErrorLogger implements IErrorLogger {

		public void reportMissingClass(ClassNotFoundException ex) {
		}

		public void reportMissingClass(ClassDescriptor classDescriptor) {
		}

		public void logError(String message) {
		}

		public void logError(String message, Throwable e) {
		}

		public void reportSkippedAnalysis(MethodDescriptor method) {
		}
	};
	
	private static class NoOpProgress implements Progress {
		public void startScanningArchives(int numArchivesToScan) {
		}

		public void doneScanningArchives() {
		}

		public void startScanningClasses(int numClassesToScan) {
		}

		public void finishClass() {
		}

		public void doneScanningClasses() {
		}

		public void finishArchive() {
		}

		public void startRecursiveDirectorySearch() {
		}

		public void doneRecursiveDirectorySearch() {
		}
		
	}
	
	private Project project;
	private String rootSourceDirectory;
	private boolean scanForNestedArchives;
	private IErrorLogger errorLogger;
	private Progress progress;
	private List<String> discoveredSourceDirectoryList;

	/**
	 * Constructor.
	 */
	public DiscoverSourceDirectories() {
		this.errorLogger = new NoOpErrorLogger();
		this.progress = new NoOpProgress();
		this.discoveredSourceDirectoryList = new LinkedList<String>();
	}

	/**
	 * Set the Project for which we want to find source directories.
	 * 
	 * @param project Project for which we want to find source directories
	 */
	public void setProject(Project project) {
		this.project = project;
	}

	/**
	 * Set the "root" source directory: we expect all of the
	 * actual source directories to be underneath it.
	 * 
	 * @param rootSourceDirectory the root source directory
	 */
	public void setRootSourceDirectory(String rootSourceDirectory) {
		this.rootSourceDirectory = rootSourceDirectory;
	}

	/**
	 * Set whether or not to scan the project for nested archives (i.e.,
	 * if there is a WAR or EAR file that contains jar files inside it.)
	 * Default is false.
	 * 
	 * @param scanForNestedArchives true if nested archives should be scanned,
	 *                              false otherwise
	 */
	public void setScanForNestedArchives(boolean scanForNestedArchives) {
		this.scanForNestedArchives = scanForNestedArchives;
	}

	/**
	 * Set the error logger to use to report errors during scanning.
	 * By default, a no-op error logger is used.
	 * 
	 * @param errorLogger error logger to use to report errors during scanning
	 */
	public void setErrorLogger(IErrorLogger errorLogger) {
		this.errorLogger = errorLogger;
	}

	/**
	 * Set the progress callback to which scanning progress should be reported.
	 * 
	 * @param progress the progress callback
	 */
	public void setProgress(Progress progress) {
		this.progress = progress;
	}

	/**
	 * Get the list of discovered source directories.
	 * These can be added to a Project.
	 * 
	 * @return list of discovered source directories.
	 */
	public List<String> getDiscoveredSourceDirectoryList() {
		return Collections.unmodifiableList(discoveredSourceDirectoryList);
	}

	/**
	 * Execute the search for source directories.
	 * 
	 * @throws edu.umd.cs.findbugs.classfile.CheckedAnalysisException
	 * @throws java.io.IOException
	 * @throws java.lang.InterruptedException
	 */
	public void execute() throws CheckedAnalysisException, IOException, InterruptedException {
		File dir = new File(rootSourceDirectory);
		if (!dir.isDirectory()) {
			throw new IOException("Path " + rootSourceDirectory + " is not a directory");
		}

		// Find all directories underneath the root source directory
		progress.startRecursiveDirectorySearch();
		RecursiveFileSearch rfs = new RecursiveFileSearch(rootSourceDirectory, new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		rfs.search();
		progress.doneRecursiveDirectorySearch();
		List<String> candidateSourceDirList = rfs.getDirectoriesScanned();
		
		// Build the classpath
		IClassPath classPath = null;
		try {
			IClassFactory factory = ClassFactory.instance();
			IClassPathBuilder builder = factory.createClassPathBuilder(errorLogger);

			classPath = buildClassPath(builder, factory);

			// From the application classes, find the full list of
			// fully-qualified source file names.
			List<String> fullyQualifiedSourceFileNameList = findFullyQualifiedSourceFileNames(builder, classPath);

			// Attempt to find source directories for all source files,
			// and add them to the discoveredSourceDirectoryList
			if (DEBUG) {
				System.out.println("looking for " + fullyQualifiedSourceFileNameList.size() + " files");
			}
			findSourceDirectoriesForAllSourceFiles(fullyQualifiedSourceFileNameList, candidateSourceDirList);
		} finally {
			if (classPath != null) {
				classPath.close();
			}
		}
	}

	private IClassPath buildClassPath(IClassPathBuilder builder, IClassFactory factory) throws InterruptedException, IOException, CheckedAnalysisException {
		
		progress.startScanningArchives(project.getFileCount());
		
		for (String path : project.getFileList()) {
			builder.addCodeBase(factory.createFilesystemCodeBaseLocator(path), true);
		}

		for (String path : project.getAuxClasspathEntryList()) {
			builder.addCodeBase(factory.createFilesystemCodeBaseLocator(path), false);
		}

		IClassPath classPath = factory.createClassPath();

		builder.build(classPath, progress);
		
		progress.doneScanningArchives();
		
		return classPath;
	}

	private String findFullyQualifiedSourceFileName(IClassPath classPath, ClassDescriptor classDesc) throws IOException, CheckedAnalysisException {
		try {
			// Open and parse the class file to attempt
			// to discover the source file name.
			ICodeBaseEntry codeBaseEntry = classPath.lookupResource(classDesc.toResourceName());

			ClassParserUsingASM classParser = new ClassParserUsingASM(new ClassReader(codeBaseEntry.openResource()), classDesc, codeBaseEntry);

			ClassInfo.Builder classInfoBuilder = new ClassInfo.Builder();
			classParser.parse(classInfoBuilder);
			ClassInfo classInfo = classInfoBuilder.build();

			// Construct the fully-qualified source file name
			// based on the package name and source file name.
			String packageName = classDesc.getPackageName();
			String sourceFile = classInfo.getSource();

			if (!packageName.equals("")) {
				packageName = packageName.replace('.', '/');
				packageName += "/";
			}

			String fullyQualifiedSourceFile = packageName + sourceFile;

			return fullyQualifiedSourceFile;
		} catch (CheckedAnalysisException e) {
			errorLogger.logError("Could scan class " + classDesc.toDottedClassName(), e);
			throw e;
		} finally {
			progress.finishClass();
		}
	}

	private List<String> findFullyQualifiedSourceFileNames(IClassPathBuilder builder, IClassPath classPath) {

		List<ClassDescriptor> appClassList = builder.getAppClassList();

		progress.startScanningClasses(appClassList.size());

		List<String> fullyQualifiedSourceFileNameList = new LinkedList<String>();

		for (ClassDescriptor classDesc : appClassList) {
			try {
				String fullyQualifiedSourceFileName = findFullyQualifiedSourceFileName(classPath, classDesc);
				fullyQualifiedSourceFileNameList.add(fullyQualifiedSourceFileName);
			} catch (IOException e) {
				errorLogger.logError("Couldn't scan class " + classDesc.toDottedClassName(), e);
			} catch (CheckedAnalysisException e) {
				errorLogger.logError("Couldn't scan class " + classDesc.toDottedClassName(), e);
			}
		}
		
		progress.doneScanningClasses();

		return fullyQualifiedSourceFileNameList;
	}

	private void findSourceDirectoriesForAllSourceFiles(List<String> fullyQualifiedSourceFileNameList, List<String> candidateSourceDirList) {

		Set<String> sourceDirsFound = new HashSet<String>();

		// For each source file discovered, try to locate it in one of
		// the candidate source directories.
		for (String fullyQualifiedSourceFileName : fullyQualifiedSourceFileNameList) {

			checkCandidateSourceDirs:
			for (String candidateSourceDir : candidateSourceDirList) {
				String path = candidateSourceDir + File.separatorChar + fullyQualifiedSourceFileName;
				File f = new File(path);
				if (DEBUG) {
					System.out.print("Checking " + f.getPath() + "...");
				}
				
				boolean found = f.exists() && !f.isDirectory();
				
				if (DEBUG) {
					System.out.println(found ? "FOUND" : "not found");
				}
				
				if (found) {
					// Bingo!
					if (sourceDirsFound.add(candidateSourceDir)) {
						discoveredSourceDirectoryList.add(candidateSourceDir);
						sourceDirsFound.add(candidateSourceDir);
					}
					break checkCandidateSourceDirs;
					
				}
			}
		}
	}
	
	/**
	 * Just for testing.
	 */
	public static void main(String[] args) throws IOException, CheckedAnalysisException, InterruptedException {
		if (args.length != 2) {
			System.err.println("Usage: " + DiscoverSourceDirectories.class.getName() + " <project file> <root source dir>");
			System.exit(1);
		}
		
		Project project = Project.readProject(args[0]);
		
		IErrorLogger errorLogger = new IErrorLogger() {

			public void reportMissingClass(ClassNotFoundException ex) {
				String className = ClassNotFoundExceptionParser.getMissingClassName(ex);
				if (className != null) {
					logError("Missing class: " + className);
				} else {
					logError("Missing class: " + ex);
				}
			}

			public void reportMissingClass(ClassDescriptor classDescriptor) {
				logError("Missing class: " + classDescriptor.toDottedClassName());
			}

			public void logError(String message) {
				System.err.println("Error: " + message);
			}

			public void logError(String message, Throwable e) {
				logError(message + ": " + e.getMessage());
			}

			public void reportSkippedAnalysis(MethodDescriptor method) {
				logError("Skipped analysis of method " + method.toString());
			}
			
		};
		
		DiscoverSourceDirectories.Progress progress = new DiscoverSourceDirectories.Progress() {

			public void startRecursiveDirectorySearch() {
				System.out.print("Scanning directories...");
				System.out.flush();
			}

			public void doneRecursiveDirectorySearch() {
				System.out.println("done");
			}

			public void startScanningArchives(int numArchivesToScan) {
				System.out.print("Scanning " + numArchivesToScan + " archives..");
				System.out.flush();
			}

			public void doneScanningArchives() {
				System.out.println("done");
			}

			public void startScanningClasses(int numClassesToScan) {
				System.out.print("Scanning " + numClassesToScan + " classes...");
				System.out.flush();
			}

			public void finishClass() {
				System.out.print(".");
				System.out.flush();
			}

			public void doneScanningClasses() {
				System.out.println("done");
			}

			public void finishArchive() {
				System.out.print(".");
				System.out.flush();
			}
		};
		
		DiscoverSourceDirectories discoverSourceDirectories = new DiscoverSourceDirectories();
		discoverSourceDirectories.setProject(project);
		discoverSourceDirectories.setRootSourceDirectory(args[1]);
		discoverSourceDirectories.setErrorLogger(errorLogger);
		discoverSourceDirectories.setProgress(progress);
		
		discoverSourceDirectories.execute();
		
		System.out.println("Found source directories:");
		for (String srcDir : discoverSourceDirectories.getDiscoveredSourceDirectoryList()) {
			System.out.println("  " + srcDir);
		}
	}
}
