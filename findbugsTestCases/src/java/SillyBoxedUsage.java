
public class SillyBoxedUsage {
	public String testBad1(int value) {
		return new Integer(value).toString();
	}

	public String testGood1(int value) {
		return Integer.toString(value);
	}

	public String testBad2(float value) {
		return new Float(value).toString();
	}

	public String testGood2(float value) {
		return Float.toString(value);
	}

	public String testBad3(double value) {
		return new Double(value).toString();
	}

	public String testGood3(double value) {
		return Double.toString(value);
	}
}