/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.plan.ExecutionPlan;

import junit.framework.TestCase;

/**
 * Abstract base class for TestCase classes that need to
 * run in the context of a FindBugs2 object doing a full
 * execution.  Ensures that things like AnalysisCache,
 * AnalysisContext, etc. are fully initialized.
 * 
 * <p> Is this mock objects?  Or is this just a hack?
 * Probably the latter :-)
 * 
 * @author David Hovemeyer
 */
public abstract class FindBugsTestCase extends TestCase {
	/**
	 * Data of an empty class in the default package called "Empty".
	 */
	public static final byte[] EMPTY_CLASS_DATA = {
		(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe,
		(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x32,
		(byte) 0x00, (byte) 0x0d, (byte) 0x0a, (byte) 0x00,
		(byte) 0x03, (byte) 0x00, (byte) 0x0a, (byte) 0x07,
		(byte) 0x00, (byte) 0x0b, (byte) 0x07, (byte) 0x00,
		(byte) 0x0c, (byte) 0x01, (byte) 0x00, (byte) 0x06,
		(byte) 0x3c, (byte) 0x69, (byte) 0x6e, (byte) 0x69,
		(byte) 0x74, (byte) 0x3e, (byte) 0x01, (byte) 0x00,
		(byte) 0x03, (byte) 0x28, (byte) 0x29, (byte) 0x56,
		(byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x43,
		(byte) 0x6f, (byte) 0x64, (byte) 0x65, (byte) 0x01,
		(byte) 0x00, (byte) 0x0f, (byte) 0x4c, (byte) 0x69,
		(byte) 0x6e, (byte) 0x65, (byte) 0x4e, (byte) 0x75,
		(byte) 0x6d, (byte) 0x62, (byte) 0x65, (byte) 0x72,
		(byte) 0x54, (byte) 0x61, (byte) 0x62, (byte) 0x6c,
		(byte) 0x65, (byte) 0x01, (byte) 0x00, (byte) 0x0a,
		(byte) 0x53, (byte) 0x6f, (byte) 0x75, (byte) 0x72,
		(byte) 0x63, (byte) 0x65, (byte) 0x46, (byte) 0x69,
		(byte) 0x6c, (byte) 0x65, (byte) 0x01, (byte) 0x00,
		(byte) 0x0a, (byte) 0x45, (byte) 0x6d, (byte) 0x70,
		(byte) 0x74, (byte) 0x79, (byte) 0x2e, (byte) 0x6a,
		(byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x0c,
		(byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x05,
		(byte) 0x01, (byte) 0x00, (byte) 0x05, (byte) 0x45,
		(byte) 0x6d, (byte) 0x70, (byte) 0x74, (byte) 0x79,
		(byte) 0x01, (byte) 0x00, (byte) 0x10, (byte) 0x6a,
		(byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2f,
		(byte) 0x6c, (byte) 0x61, (byte) 0x6e, (byte) 0x67,
		(byte) 0x2f, (byte) 0x4f, (byte) 0x62, (byte) 0x6a,
		(byte) 0x65, (byte) 0x63, (byte) 0x74, (byte) 0x00,
		(byte) 0x21, (byte) 0x00, (byte) 0x02, (byte) 0x00,
		(byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
		(byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00,
		(byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x00,
		(byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x1d, (byte) 0x00, (byte) 0x01, (byte) 0x00,
		(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x05, (byte) 0x2a, (byte) 0xb7, (byte) 0x00,
		(byte) 0x01, (byte) 0xb1, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x07,
		(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06,
		(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01,
		(byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x09,
	};

    private final class TestRunnerThread extends Thread {
	    private RunnableWithExceptions runnable;
	    private JUnitDetectorAdapter detectorAdapter;

	    private TestRunnerThread(RunnableWithExceptions runnable) {
		    this.runnable = runnable;
	    }
	    
	    /**
         * @return Returns the detectorAdapter.
         */
        public JUnitDetectorAdapter getDetectorAdapter() {
	        return detectorAdapter;
        }

	    /* (non-Javadoc)
	     * @see java.lang.Thread#run()
	     */
	    @Override
	    public void run() {
	    	try {
	    		runTest(runnable);
	    	} catch (Exception e) {
	    		// Hmm...
	    		System.err.println("Exception running test:");
	    		e.printStackTrace();
	    	}
	    }
		
		private void runTest(RunnableWithExceptions runnable) throws IOException, InterruptedException {
			// Create temporary directory in filesystem
			File tmpdir = File.createTempFile("fbtest", null);
			if (!tmpdir.delete() || !tmpdir.mkdir()) {
				throw new IOException("Could not create temp dir");
			}
			
			File tmpfile = null;

			try {
				// Create a class file to analyze
				tmpfile = createEmptyClassFile(tmpdir);
				
				// Unfortunately there's quite a bit of gobbledygook required
				// to set up a FindBugs2.

				FindBugs2 engine = new FindBugs2();
				
				engine.setBugReporter(new PrintingBugReporter());

				// Analyze the temporary directory we just created
				Project project = new Project();
				project.addFile(tmpdir.getAbsolutePath());

				engine.setProject(project);

				DetectorFactoryCollection dfc = new DetectorFactoryCollection();
				DetectorFactoryCollection.resetInstance(dfc);

				Plugin fakePlugin = new Plugin("edu.umd.cs.findbugs.fakeplugin", null);
				fakePlugin.setEnabled(true);

				dfc.setPlugins(new Plugin[]{fakePlugin});

				DetectorFactory detectorFactory =
					new DetectorFactory(fakePlugin, JUnitDetectorAdapter.class, true, "fast", "", "");
				fakePlugin.addDetectorFactory(detectorFactory);
				dfc.registerDetector(detectorFactory);
				if (!dfc.factoryIterator().hasNext() || !fakePlugin.detectorFactoryIterator().hasNext()) {
					throw new IllegalStateException();
				}

				engine.setDetectorFactoryCollection(dfc);

				engine.setUserPreferences(UserPreferences.createDefaultUserPreferences());
				
				JUnitDetectorAdapter.setRunnable(runnable);

				engine.execute();
				
				// Get a handle to the JUnitDetectorAdapter, since it is the
				// object that knows whether or not the test code actually passed
				// or failed.
				detectorAdapter = JUnitDetectorAdapter.instance();
			} finally {
				if (tmpfile != null) {
					tmpfile.delete();
				}
				tmpdir.delete();
			}
		}

		/**
	     * @param tmpdir
		 * @throws IOException 
	     */
	    private File createEmptyClassFile(File tmpdir) throws IOException {
	    	File outFile = new File(tmpdir, "Empty.class");
	    	OutputStream out = new FileOutputStream(outFile);
	    	try {
	    		out.write(EMPTY_CLASS_DATA);
	    	} finally {
	    		out.close();
	    	}
	    	return outFile;
	    }
    }

	protected void executeFindBugsTest(final RunnableWithExceptions runnable) throws Throwable {
		TestRunnerThread thread = new TestRunnerThread(runnable);
		
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			throw new IllegalStateException();
		}

		if (thread.getDetectorAdapter() == null) {
			throw new IllegalStateException("Test code did not complete");
		}
		thread.getDetectorAdapter().finishTest();
	}	
}
