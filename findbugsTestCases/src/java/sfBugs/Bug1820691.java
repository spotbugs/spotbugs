package sfBugs;

public class Bug1820691 {
	private static final String[] NO_STRINGS = new String[0];

	Bug1820691 gorp[] = new Bug1820691[0];

	public String[] getGorps() {
		if (gorp == null || gorp.length == 0) {
			return NO_STRINGS;
		}
		String[] result = new String[gorp.length];
		for (int i = 0; i < gorp.length; i++)
			result[i] = gorp[i].toString();
		return result;
	}
}
