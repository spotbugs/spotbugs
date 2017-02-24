/* ****************************************
 * $Id$
 * SF bug 2010156:
 *   Line numbers not reported/highlighted for certain
 *   error types (some of which report errors in textui mode)
 *
 * JVM:  1.5.0_16 (OS X, PPC)
 * FBv:  1.3.7-dev-20081212
 *
 * Certain errors reported by the GUI do not highlight the code
 * with which they are associated, nor are line numbers reported;
 * in certain cases, the line numbers for the same bugs /are/
 * reported when using the text UI.
 *
 * **************************************** */

package sfBugs.b;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Comparator;

/* ********************
 * use of class name identical to superclass (but with different package)
 * throws SE_NO_SUITABLE_CONSTRUCTOR_FOR_EXTERNALIZATION warning
 * with line numbers, but no lines are reported/highlighted
 * ******************** */
public class bug2010156 extends sfBugs.a.bug2010156 {
    private int i, j, k;

    protected int mask = 0;

    bug2010156(int a, int b, int c) {
        i = a;
        j = b;
        k = c;
    }

    public int a() {
        return i + j + k;
    }

    /* ********************
     * definition of non-static inner class throws SIC_INNER_SHOULD_BE_STATIC
     * warning with line numbers, but no lines are reported/highlighted
     * 
     * based on
     * findbugsTestCases/src/java/AccidentalNonConstructorInInnerClass.java
     * ********************
     */
    public class subclass extends bug2010156 {
        /* ********************
         * masking parent 'mask' variable throws MF_CLASS_MASKS_FIELD warning
         * with NO line numbers, and so cannot be highlighted
         * 
         * based on findbugsTestCases/src/java/MaskMe.java ********************
         */
        protected int mask = 1;

        subclass() {
            super(0, 0, 0);
        }

        public int getMask() {
            return mask;
        }

    }

    public static class extern implements Externalizable {
        protected int var;

        /* ********************
         * lack of void constructor throws
         * SE_NO_SUITABLE_CONSTRUCTOR_FOR_EXTERNALIZATION warning with line
         * numbers, but no lines are reported/highlighted
         * 
         * based on findbugsTestCases/src/java/ExternalizableTest.java
         * ********************
         */
        extern(int i) {
            var = i;
        }

        public int getVar() {
            return var;
        }

        @Override
        public void readExternal(ObjectInput in) {
            var = 0;
        }

        @Override
        public void writeExternal(ObjectOutput out) {
            var = 1;
        }
    }

    static class serial extends sfBugs.b.bug2010156 implements Serializable {
        /* ********************
         * lack of void constructor throws SE_NO_SUITABLE_CONSTRUCTOR warning
         * with line numbers, but no lines are reported/highlighted
         * 
         * based on findbugsTestCases/src/java/CloneIdiom1.java
         * ********************
         */
        serial() {
            super(1, 2, 3);
        }
    }

    static class compare implements Comparator {
        /* ********************
         * lack of Serializable implementation throws
         * SE_COMPARATOR_SHOULD_BE_SERIALIZABLE warning with line numbers, but
         * no lines are reported/highlighted
         * 
         * based on findbugsTestCases/src/java/Comparador.java
         * ********************
         */
        @Override
        public int compare(Object arg0, Object arg1) {
            return arg0.hashCode() - arg1.hashCode();
        }
    }

    static class clone implements Cloneable {
        /* ********************
         * lack of clone method throws CN_IDIOM warning with line numbers, but
         * no lines are reported/highlighted
         * 
         * based on findbugsTestCases/src/java/CloneIdiom1.java
         * ********************
         */
    }

}

/* **********************************************************************
 * Identified instances in findbugsTestCases code where Text UI reports line
 * numbers, but no highlighting is done by GUI:
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/CloneIdiom1.java GUI Bug Pattern: Class implements
 * Cloneable but does not define or use clone method Text UI Message: M B
 * CN_IDIOM CN: Class CloneIdiom1 implements Cloneable but \ does not define or
 * use clone method At CloneIdiom1.java:[line 1]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/AbstractMissingHashCode.java GUI Bug Pattern:
 * Class inherits equals() and uses Object.hashCode() Text UI Message: M B
 * HE_INHERITS_EQUALS_USE_HASHCODE HE: \
 * AbstractMissingHashCode$StillMissingHashCode inherits equals and uses \
 * Object.hashCode() At AbstractMissingHashCode.java:[line 11]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/UseOfNonHashableClassInHashDataStructure.java GUI
 * Bug Pattern: Signature declares use of unhashable class in hashed construct
 * Text UI Message: H C HE_USE_OF_UNHASHABLE_CLASS HE: \
 * UseOfNonHashableClassInHashDataStructure doesn't define a hashCode() method \
 * but is used in a hashed data structure \ At
 * UseOfNonHashableClassInHashDataStructure.java:[line 23]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/Bar.java GUI Bug Pattern: Initialization
 * circularity Text UI Message: M D IC_INIT_CIRCULARITY IC: Initialization
 * circularity between \ Bar and Foo At Bar.java:[lines 1-5]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/hashCODEnoEQUALS.java GUI Bug Pattern: Class names
 * should start with an upper case letter Text UI Message: M B
 * NM_CLASS_NAMING_CONVENTION Nm: The class name \ hashCODEnoEQUALS doesn't
 * start with an upper case letter \ At hashCODEnoEQUALS.java:[lines 3-19]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/FalseException.java GUI Bug Pattern: Class is not
 * derived from an Exception, even though it is named as such Text UI Message: M
 * B NM_CLASS_NOT_EXCEPTION Nm: Class FalseException is not \ derived from an
 * Exception, even though it is named as such \ At FalseException.java:[line 1]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/ReadObject.java GUI Bug Pattern: Class's
 * readObject() method is synchronized Text UI Message: M M RS_READOBJECT_SYNC
 * RS: ReadObject's readObject method is \ synchronized At
 * ReadObject.java:[lines 11-16]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/Comparador.java GUI Bug Pattern: Comparator
 * doesn't implement Serializable Text UI Message: M B
 * SE_COMPARATOR_SHOULD_BE_SERIALIZABLE Se: Comparador \ implements Comparator
 * but not Serializable At Comparador.java:[lines 5-15]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/PublicReadObject.java GUI Bug Pattern: Class is
 * Serializable, but doesn't define serialVersionUID Text UI Message: M B
 * SE_NO_SERIALVERSIONID SnVI: PublicReadObject is \ Serializable; consider
 * declaring a serialVersionUID \ At PublicReadObject.java:[lines 5-13]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/Serializable2.java GUI Bug Pattern: Class is
 * Serializable but its superclass doesn't define a void constructor Text UI
 * Message: H B SE_NO_SUITABLE_CONSTRUCTOR Se: Serializable2$Inner is \
 * Serializable but its superclass doesn't define an accessible void constructor
 * \ At Serializable2.java:[lines 20-21]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/ExternalizableTest.java GUI Bug Pattern: Class is
 * Externalizable but doesn't define a void constructor Text UI Message: H B
 * SE_NO_SUITABLE_CONSTRUCTOR_FOR_EXTERNALIZATION Se: \ ExternalizableTest is
 * Externalizable but doesn't define a void constructor \ At
 * ExternalizableTest.java:[lines 12-34]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/AccidentalNonConstructorInnerClass.java GUI Bug
 * Pattern: Should be a static inner class Text UI Message: M P
 * SIC_INNER_SHOULD_BE_STATIC SIC: Should \
 * AccidentalNonConstructorInInnerClass$Report be a _static_ inner class? \ At
 * AccidentalNonConstructorInInnerClass.java:[lines 3-7]
 * 
 * ---------------------------------------- Filename:
 * findbugsTestCases/src/java/sfBugs/b/Bug1718199.java GUI Bug Pattern: Class
 * names shouldn't shadow simple name of superclass Text UI Message: H B
 * NM_SAME_SIMPLE_NAME_AS_SUPERCLASS Nm: The class name \ sfBugs.b.Bug1718199
 * shadows the simple name of the superclass \ sfBugs.a.Bug1718199 At
 * Bug1718199.java:[lines 3-5]
 * 
 * ----------------------------------------
 * 
 * *********************************************************************
 */
