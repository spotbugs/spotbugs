package edu.umd.cs.findbugs.workflow;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.TestCase;
import edu.umd.cs.findbugs.AppVersion;

public class FindSeqNumTest extends TestCase {

	Map<String, AppVersion> versionNames;
	SortedMap<Long, AppVersion> timeStamps;
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		versionNames = new HashMap<String, AppVersion>();
		timeStamps = new TreeMap<Long, AppVersion>();
		Set<AppVersion> versions = new HashSet<AppVersion>();
		SimpleDateFormat format = new SimpleDateFormat("MMMMM dd, yyyy");
		versions.add(new AppVersion(0, format.parse("June 1, 2005"), "v1.0"));
		versions.add(new AppVersion(1, format.parse("June 10, 2005"), "v1.1"));
		versions.add(new AppVersion(2, format.parse("June 20, 2005"), "v2.0"));

		for(AppVersion v : versions) {
			versionNames.put(v.getReleaseName(), v);
			timeStamps.put(v.getTimestamp(), v);
		}
	}

	public void test0() {
		assertEquals(0,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "0", true, 3));
	}
	public void testminusOne() {
		assertEquals(3,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "-1", true, 3));
	}
	public void testminusTwo() {
		assertEquals(2,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "-2", true, 3));
	}
	public void testLast() {
		assertEquals(3,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "last", true, 3));
	}
	public void testlastVersion() {
		assertEquals(3,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "lastVersion", true, 3));
	}
	public void test1() {
		assertEquals(1,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "1", true, 3));
	}
	public void testV1_0() {
		assertEquals(0,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "v1.0", true, 3));
	}
	public void testV1_1() {
		assertEquals(1,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "v1.1", true, 3));
	}
	public void testV2_0() {
		assertEquals(2,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "v2.0", true, 3));
	}
	public void testV2_1() {
		try {
		Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "v2.1", true, 0);
		assertTrue("Didn't throw IllegalArgumentException", false);
		} catch (IllegalArgumentException e) {
			// we expected this;
		}
	}
	public void testAfterMay5() {
		assertEquals(0,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "5/5/2005", true, 3));
	}
	public void testAfterJune5() {
		assertEquals(1,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "6/5/2005", true, 3));
	}
	public void testAfterJune15() {
		assertEquals(2,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "June 15, 2005", true, 3));
	}
	public void testAfterJune25() {
		assertEquals(Long.MAX_VALUE,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "June 25, 2005", true, 3));
	}
	public void testBeforeMay5() {
		assertEquals(Long.MIN_VALUE,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "5/5/2005", false, 3));
	}
	public void testBeforeJune5() {
		assertEquals(0,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "6/5/2005", false, 3));
	}
	public void testBeforeJune15() {
		assertEquals(1,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "June 15, 2005", false, 3));
	}
	public void testBeforeJune25() {
		assertEquals(2,Filter.FilterCommandLine.getVersionNum(versionNames, timeStamps, "June 25, 2005", false, 3));
	}

}
