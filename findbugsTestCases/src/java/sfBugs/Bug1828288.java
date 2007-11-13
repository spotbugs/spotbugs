package sfBugs;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class Bug1828288 {

	 @CheckForNull
	 private Object field = null;

	 public void init() {
	   field = new Object();
	 }
	 @CheckForNull
	 public Object nullx() {
		 return null;
	 }

	 @Override
	 public String toString() {
	   return field.toString(); // (*)
	 }
	
	 
	 public String toString2() {
	   return nullx().toString(); // (*)
	 }


}
