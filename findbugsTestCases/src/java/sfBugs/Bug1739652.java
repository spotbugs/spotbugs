package sfBugs;

/**
 * Submitted By: Yvan Norsa
 * Summary:
 * 
 * FindBugs (1.2.1-rc2) finds a DE_MIGHT_IGNORE when compiling with debug info
 * (that is, the default javac options) the following file :
 * 
 * final class A {
 * public void foo() {
 *  try {
 *   Class.forName("A");
 *  } catch (ClassNotFoundException classEx) {
 *  }
 *  }
 * }
 *
 * However, if you put the catch block '}' on the same line as the opening
 * '{', no bug is found.
 * 
 * Furthermore, if you compile with -g:none, the '}' position won't have any
 * influence on the number of bugs found.
 * 
 * I had a look at the class file when compiled with default debug options,
 * and the file size is different, probably because of the line numbers info.
 * But the bytecode obtained with "javap -c" is the same..
 */
public final class Bug1739652 {
    public void foo() {
        try {
            Class.forName("Bug1739652");
        } catch (ClassNotFoundException classEx) {
        }
    }
}
