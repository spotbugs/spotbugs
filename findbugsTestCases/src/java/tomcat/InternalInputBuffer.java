package tomcat;

// Inspired by false positive in org.apache.coyote.http11.InternalInputBuffer
public class InternalInputBuffer {

	byte [] buf = new byte[100];

	Object foo() {
		return new Object();
	}
	int falsePositive(boolean b) {
		boolean colon = false;
		int result;
		Object headerValue = null;
		int pos = 0;
		// this should be a do-while rather than a while-do loop
		while (!colon) {
			if (buf[pos] == 58) {
				headerValue = foo();
				colon = true;
			}
			pos++;
		}
		if (b) result = 1;
		else result = 2;
		return result + headerValue.hashCode();
	}

}
