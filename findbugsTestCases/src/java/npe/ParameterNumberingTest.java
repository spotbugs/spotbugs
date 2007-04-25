package npe;

public class ParameterNumberingTest {

	int f(Object x) {
		return x.hashCode();
    }

	int g() {
		int i = f(null);
        Object x = null;
		if (i != 0) x = "hello";
		int j = f(x);
		Object y = null;
        if (i != 0) y = "bye";
		int k;
		if (j > 0)
			k = f(y);
        else k = -f(y);
		return i+j+k;
	}

}
