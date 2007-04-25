package npe;

import java.text.Format;
import java.text.ParseException;

abstract public class SecondKaboom {
	abstract void fail();

	abstract void assertTrue(boolean b);

	abstract void assertEquals(Object o1, Object o2);

	protected void parseHelperSuccess(Format f, String parseString, Object expectedResult) {
		Object result;
		// First check the parseObject(String) method.
        try {
			result = f.parseObject(parseString);
			result.equals(expectedResult);
			if (expectedResult == null) {
                assertTrue(result == expectedResult);
			} else {
				assertEquals(result, expectedResult);
			}
        } catch (ParseException e) {
			fail();
		}
	}
}
