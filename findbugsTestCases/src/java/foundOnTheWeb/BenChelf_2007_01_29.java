package foundOnTheWeb;

/**
 * Examples from http://jdj.sys-con.com/read/325148.htm
 * 
 * Title: The Paradox of Writing Perfect Code: Static code analysis versus Santa
 * Claus and the Easter Bunny
 * 
 * Author: http://jdj.sys-con.com/read/325148.htm
 * 
 * Code from structs-1.3.8, /org/apache/struts/taglib/bean/DefineTag.java
 */
public class BenChelf_2007_01_29 {

	/**
	 * From article: <blockquote>Notice that on line 171, the developer compares
	 * body against null. Unfortunately, the developer probably meant to make
	 * that comparison == instead of !=. In the case where the pointer is null,
	 * the code will skip over the assignment on line 172 and dereference the
	 * body variable on line 175. Oops. </blockquote>
	 * 
	 * Actually, that analysis is quite wrong. The problem wasn't that the
	 * developer meant to compare with ==. The problem is that the length check
	 * should have been inside the if (body != null) test.
	 * 
	 * 
	 */
	public static String listing1(String body) {
		if (body != null) {
			body = body.trim();
		}

		if (body.length() < 1) {
			body = null;
		}
		return body;
	}

	public static String listing1FixedAsSuggestedByChen(String body) {
		if (body == null) {
			body = body.trim();
		}

		if (body.length() < 1) {
			body = null;
		}
		return body;
	}

	public static String listing1FixedAsSuggestedByPugh(String body) {
		if (body != null) {
			body = body.trim();
			if (body.length() < 1) {
				body = null;
			}
		}
		return body;
	}

	/**
	 * From article: <blockquote>
	 * The struts code from the previous example has been slightly modified to introduce a data dependence between the value of body and the value of body_tracker. Notice that after the test of body against NULL, the value of body_tracker will be 5 if body is not NULL and 12 if body is NULL. As such, there's no longer a NULL dereference on line 177 because it's guarded by the check of body tracker. This example is simple enough, but may fool some simple analysis engines into reporting the defect where there really is no problem at all because there's no possible execution path that leads body to be dereferenced when NULL.
	 * </blockquote>
	 * 
	 */
	public static String listing3(String body) {

		int body_tracker = 5;
		if (body != null) {
			body = body.trim();
		} else {
			body_tracker = 12;
		}

		if (body_tracker != 12 && body.length() < 1) {
			body = null;
		}
		return body;
	}

	/**
	 * From article: <blockquote>
	 * Take a look at Listing 4. Notice that on line 173 there's an extra space before the statement. Your static code analysis tool could report that extra space as a defect, but I'm willing to bet that most of us would consider that noise. The analysis isn't wrong per se - the statements don't line up - but I just don't care. 
	 * </blockquote>
	 */
	public static String listing4(String body) {

		int body_tracker = 5;
		if (body != null) {
			body = body.trim();
			    body_tracker = 12;
		}

		if (body_tracker != 12 && body.length() < 1) {
			body = null;
		}
		return body;
	}
}
