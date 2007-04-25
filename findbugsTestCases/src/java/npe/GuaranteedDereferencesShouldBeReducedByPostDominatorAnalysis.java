package npe;

public class GuaranteedDereferencesShouldBeReducedByPostDominatorAnalysis {

	int f(Object x, boolean b) {
		int result = 0;
        if (x == null) {
			result++;
			result++;
			result++;
        }
		if (b) 
			result += x.hashCode();
		if (b)
            result += x.hashCode();
		result += x.hashCode();
		return result;
	}

}
