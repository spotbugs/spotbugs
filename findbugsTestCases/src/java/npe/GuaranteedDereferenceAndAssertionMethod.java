package npe;

public class GuaranteedDereferenceAndAssertionMethod {
	
	void jsrError() {
		
	}
	int falsePositive(Object x) {
		if (x == null) 
			jsrError();
		return x.hashCode();
		
		
	}

}
