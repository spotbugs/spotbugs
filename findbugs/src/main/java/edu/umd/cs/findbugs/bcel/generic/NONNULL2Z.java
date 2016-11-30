
package edu.umd.cs.findbugs.bcel.generic;


/** A synthetic instruction that converts a reference to a boolean value,
 * translating any nonnull value to 1 (true), and null value to 0 (false).
 *
 */
public class NONNULL2Z extends NullnessConversationInstruction {

    public NONNULL2Z() {
        super(org.apache.bcel.Constants.IMPDEP2);
    }



}
