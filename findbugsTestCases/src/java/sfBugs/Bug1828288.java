package sfBugs;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class Bug1828288 {

	 @CheckForNull
	 public Object field;

	 @Override
	 public String toString() {
	   return field.toString(); // (*)
	 }

}
