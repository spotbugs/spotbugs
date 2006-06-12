

import java.util.Comparator;

public class Comparador implements Comparator{

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	public int compare(Object arg0, Object arg1) {
		return arg0.hashCode()-arg1.hashCode();
	}
}
