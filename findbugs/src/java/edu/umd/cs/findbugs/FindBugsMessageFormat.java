package edu.umd.cs.findbugs;

public class FindBugsMessageFormat {
	private String pattern;

	public FindBugsMessageFormat(String pattern) {
		this.pattern = pattern;
	}

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
