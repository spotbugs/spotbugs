package edu.umd.cs.findbugs;

/**
 * Format the message for a BugInstance.
 * This class works in much the same way as <code>java.text.MessageFormat</code>;
 * however, each placeholder may have an optional "key" which specifies
 * how the object at that position should be formatted.
 *
 * <p> Example:
 * <pre>
 *     new FindBugsMessageFormat("BUG: {1.shortMethod} does something bad to field {2}")
 * </pre>
 * In this example, the method annotation at position 1 is formatted using
 * the string "shortMethod" as the key.  Hypothetically, this would indicate
 * that we want the short form of the method.  The field annotation at position 2
 * is formatted with the empty (default) key.
 *
 * @see BugInstance
 * @author David Hovemeyer
 */
public class FindBugsMessageFormat {
	private String pattern;

	/**
	 * Constructor.
	 * @param pattern the pattern for the message
	 */
	public FindBugsMessageFormat(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Format the message using the given array of BugAnnotations as arguments
	 * to bind to the placeholders in the pattern string.
	 * @param args the BugAnnotations used as arguments
	 * @return the formatted message
	 */
	public String format(BugAnnotation[] args) {
		String pat = pattern;
		StringBuffer result = new StringBuffer();

		while (pat.length() > 0) {
			int subst = pat.indexOf('{');
			if (subst < 0) {
				result.append(pat);
				break;
			}

			result.append(pat.substring(0, subst));
			pat = pat.substring(subst + 1);

			int end = pat.indexOf('}');
			if (end < 0)
				throw new IllegalStateException("bad pattern " + pattern);

			String substPat = pat.substring(0, end);

			int dot = substPat.indexOf('.');
			String key = "";
			if (dot >= 0) {
				key = substPat.substring(dot + 1);
				substPat = substPat.substring(0, dot);
			}

			int fieldNum;
			try {
				fieldNum = Integer.parseInt(substPat);
			} catch (NumberFormatException e) {
				throw new IllegalStateException("bad pattern " + pattern);
			}

			BugAnnotation field = args[fieldNum];
			result.append(field.format(key));

			pat = pat.substring(end + 1);
		}

		return result.toString();
	}
}

// vim:ts=4
