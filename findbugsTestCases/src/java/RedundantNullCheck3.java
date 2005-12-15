import java.io.*;
import java.util.*;

class RedundantNullCheck3 {

	public void test1() throws Exception {
		FileInputStream in = new FileInputStream("/dev/null");
		// generate a warning here
		if (in == null)
			in.close();
	}

	public void test2() throws Exception {
		FileInputStream in = new FileInputStream("/dev/null");
		// don't generate a warning here
		if (in != null)
			in.close();
	}

	public void test3() throws Exception {
		FileInputStream in = new FileInputStream("/dev/null");
		// don't generate a warning here
		if (in == null)
			throw new IllegalStateException("in is null");
	}

	public void test4() throws Exception {
		FileInputStream in = new FileInputStream("/dev/null");
		// generate a warning here
		if (in != null)
			throw new IllegalStateException("in is null");
	}

	public void test5() throws Exception {
		FileInputStream in = null;
		// generate a NP warning here, but no RCN (maybe)
		if (in == null)
			in.close();
	}

	public void test6() throws Exception {
		FileInputStream in = null;
		// generate a RCN warning here
		if (in != null)
			in.close();
	}

	public void test7() throws Exception {
		FileInputStream in = null;
		// don't generate a RCN warning here (maybe)
		if (in == null)
			throw new IllegalStateException("in is null");
	}

	public void test8() throws Exception {
		FileInputStream in = null;
		// generate a warning here
		if (in != null)
			throw new IllegalStateException("in is null");
	}

	public void test9(boolean b) throws Exception {
		FileInputStream in = null;
		if (b)
			in = new FileInputStream("/dev/null");
		// generate a warning here
		if (in == null)
			in.close();
	}

	public void test10(boolean b) throws Exception {
		FileInputStream in = null;
		if (b)
			in = new FileInputStream("/dev/null");
		// don't generate a warning here
		if (in != null)
			in.close();
	}

	public void test11(boolean b) throws Exception {
		FileInputStream in = null;
		if (b)
			in = new FileInputStream("/dev/null");
		// don't generate a warning here
		if (in == null)
			throw new IllegalStateException("in is null");
	}

	public void test12(boolean b) throws Exception {
		FileInputStream in = null;
		if (b)
			in = new FileInputStream("/dev/null");
		// generate a warning here
		if (in != null)
			throw new IllegalStateException("in is null");
	}
}
