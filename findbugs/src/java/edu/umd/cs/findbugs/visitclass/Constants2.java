/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

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
