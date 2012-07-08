package sfBugs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * Submitted By: Bhavana Summary:
 * 
 * There are two while loops which iterate through the list. The first loop just
 * prints the different objects and the second one concatenates the objects.
 * 
 * Both the while loops are quite similar. There is an if clause which compares
 * if the field retrieve is not equal to null and not equal to empty string.
 * Findbugs detects a bug at line number 18(the if in the first while loop). ES:
 * Comparison of String objects using == or! = (ES_COMPARING_STRINGS_WITH_EQ).
 * It does not report any bug or warnings for line number 27(the if in the
 * second while loop).
 * 
 * If the bug at line no 18 is fixed, Findbugs still does not report a bug or
 * warning for line 27.
 * 
 * If the first while loop is commented out then Findbugs reports the bug at
 * line 27.
 * 
 * ES: Comparison of String objects using == or! =
 * (ES_COMPARING_STRINGS_WITH_EQ).
 * 
 * In this case Findbugs could not detect the second occurrence of the same bug
 * if the first occurrence is fixed. Also, Findbugs does not point out if there
 * are multiple occurrences of the same bug.
 */
public class Bug1744457 {

    public static void main(String args[]) {
        Bug1744457 b = new Bug1744457();
        b.justFirst();
        b.justSecond();
        b.both();
    }

    @ExpectWarning("ES_COMPARING_STRINGS_WITH_EQ,SBSC_USE_STRINGBUFFER_CONCATENATION")
    public void both() {
        List<String> A = new ArrayList<String>();
        A.add("Alex");
        A.add("john");
        A.add("lily");
        A.add("tracy");
        Iterator<String> it = A.iterator();

        while (it.hasNext()) {
            String retrieve = it.next();
            if (retrieve != null && !retrieve.equals(""))
                System.out.println(retrieve);
        }

        it = A.iterator();
        String add = "";
        while (it.hasNext()) {
            String retrieve = it.next();
            if (retrieve != null && retrieve != "")
               add += retrieve;
            System.out.println(add);
        }
    }

    public void justFirst() {
        List<String> A = new ArrayList<String>();
        A.add("Alex");
        A.add("john");
        A.add("lily");
        A.add("tracy");
        Iterator<String> it = A.iterator();

        while (it.hasNext()) {
            String retrieve = it.next();
            if (retrieve != null && !retrieve.equals(""))
                System.out.println(retrieve);
        }
    }

    @ExpectWarning("ES_COMPARING_STRINGS_WITH_EQ,SBSC_USE_STRINGBUFFER_CONCATENATION")
    public void justSecond() {
        List<String> A = new ArrayList<String>();
        A.add("Alex");
        A.add("john");
        A.add("lily");
        A.add("tracy");
        Iterator <String>it = A.iterator();

        it = A.iterator();
        String add = "";
        while (it.hasNext()) {
            String retrieve = it.next();
            if (retrieve != null && retrieve != "")
                add += retrieve;
            System.out.println(add);
        }
    }

}
