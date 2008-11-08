package equals;

import java.io.Serializable;

public class ArrayEquality {

	boolean reportProblemsWithArrayEquality(String[] a, String b) {
		return a.equals(b);
	}

	boolean reportProblemsWithArrayEquality2(String[] a, String b) {
		return  b.equals(a);
	}
	boolean reportProblemsWithArrayEquality3(String[] a, String []b) {
		return a.equals(b);
	}
	boolean reportProblemsWithArrayEquality4(String[][] a, String []b) {
		return a.equals(b);
	}
	boolean reportProblemsWithArrayEquality5(String[] a, String [][]b) {
		return a.equals(b);
	}
	boolean reportProblemsWithArrayEquality6(String[] a, int []b) {
		return a.equals(b);
	}
	boolean reportProblemsWithArrayEquality7(int[] a, String []b) {
		return a.equals(b);
	}
	boolean reportProblemsWithArrayEquality8(StringBuffer[] a, String []b) {
		return a.equals(b);
	}
	boolean reportProblemsWithArrayEqualityFalsePositive1(String[] a, Serializable b) {
		return a.equals(b) || b.equals(a);
	}
	boolean reportProblemsWithArrayEqualityFalsePositive2(String[] a, Cloneable b) {
		return a.equals(b) || b.equals(a);
	}

}
