
package edu.umd.cs.findbugs.bcel.generic;


/** A synthetic instruction that converts a reference to a boolean value,
 * translating null to 1 (true), and any nonnull value to 0 (false).
 *
 */
public class NULL2Z extends NullnessConversationInstruction {


    public NULL2Z() {
        super(org.apache.bcel.Constants.IMPDEP1);
    }



}
