package sfBugs;

public class Bug1933930 {
	//public static final String US_ENGLISH_MAPPING_STRING = "";
	//public static final char[] US_ENGLISH_MAPPING = US_ENGLISH_MAPPING_STRING.toCharArray();

	public static final String US_ENGLISH_MAPPING_STRING = "01230120022455012623010202";

	/**
	 * This is a default mapping of the 26 letters used in US English. A
	value of <code>0</code> for a letter position
	 * means do not encode.
	 * 
	 * @see Soundex#Soundex(char[])
	 */
	public static final char[] US_ENGLISH_MAPPING = US_ENGLISH_MAPPING_STRING.toCharArray();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(US_ENGLISH_MAPPING);
	}
}

