package edu.umd.cs.pugh.visitclass;
/**
 * Constants for the project, mostly defined in the JVM specification.
 *
 * @version 971002
 * @author  <A HREF="http://www.inf.fu-berlin.de/~dahm">M. Dahm</A>
 */
public interface Constants2 extends org.apache.bcel.Constants
{


  public static final byte M_INT    = 1;
  public static final byte M_UINT    = 2;
  public static final byte M_CP     = 3;
  public static final byte M_R      = 4;
  public static final byte M_BR     = 5;
  public static final byte M_PAD     = 6;
  /**
   * Meaning of bytecode operands
   */

  public static final short[][] MEANING_OF_OPERANDS = {
  // 0   1   2   3   4   5   6   7   8   9
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {M_INT}, {M_INT}, {M_CP}, {M_CP}, 
    {M_CP}, {M_R}, {M_R}, {M_R}, {M_R}, {M_R}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {M_R}, {M_R}, {M_R}, {M_R}, {M_R}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
// 130   1   2   3   4   5   6   7   8   9
    {}, {}, {M_R, M_INT}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR},
    {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_R}, 
// 170   1   2   3   4   5   6   7   8   9
    {}, {}, {}, {}, {}, {}, {}, {}, {M_CP}, {M_CP}, 
    {M_CP}, {M_CP}, {M_CP}, {M_CP}, {M_CP}, 
	 {M_CP, M_PAD, M_PAD}, {}, {M_CP}, {M_UINT}, {M_CP}, 
// 190   1       2       3   4   5       6                7       8       9
    {}, {}, {M_CP}, {M_CP}, {}, {}, {M_PAD}, {M_CP, M_UINT}, {M_BR}, {M_BR}, 
    {M_BR}, {M_BR}, {}, {}, {}, {}, {},
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
  };




}
