package npe;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class InsideCatchBlock {
	public int doNotReportCatchNullPointerException(@CheckForNull Object x) {
		try {
			return x.hashCode();
		} catch (NullPointerException e) {
			return 42;
		}
	}
	public int doNotReportCatchRuntimeException(@CheckForNull Object x) {
		try {
			return x.hashCode();
		} catch (RuntimeException e) {
			return 42;
		}
	}
	public int doNotReportCatchException(@CheckForNull Object x) {
		try {
			return x.hashCode();
		} catch (Exception e) {
			return 42;
		}
	}

}
