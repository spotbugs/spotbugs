/**
 * A high-level abstraction layer for codebases, classes, and components of
 * classes (methods, fields, instructions, etc.). The idea is to decouple
 * FindBugs as much as possible from any particular bytecode framework (BCEL,
 * ASM, etc.)
 * 
 * Implementations of these interfaces may be found in the
 * edu.umd.cs.findbugs.classfile.impl package.  Instances should be created
 * using the ClassFactory singleton in that package.
 */
@javax.annotation.ParametersAreNonnullByDefault
@edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters(edu.umd.cs.findbugs.annotations.NonNull.class)
package edu.umd.cs.findbugs.classfile;