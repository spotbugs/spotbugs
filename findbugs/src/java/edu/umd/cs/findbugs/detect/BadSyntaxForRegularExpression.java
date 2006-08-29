/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005-2006 University of Maryland
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

package edu.umd.cs.findbugs.detect;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.StatelessDetector;

public class BadSyntaxForRegularExpression 
extends BytecodeScanningDetector {

    BugReporter bugReporter;

    public BadSyntaxForRegularExpression(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }



    @Override
    public void visit(JavaClass obj) {
    }

    @Override
    public void visit(Method obj) {
    }

    OpcodeStack stack = new OpcodeStack();
    @Override
    public void visit(Code obj) {
	stack.resetForMethodEntry(this);
        super.visit(obj);
    }

    private void singleDotPatternWouldBeSilly(int stackDepth, boolean ignorePasswordMasking) {
    	if (ignorePasswordMasking && stackDepth != 1) 
    		throw new IllegalArgumentException("Password masking requires stack depth 1, but is " + stackDepth);
        if (stack.getStackDepth() < stackDepth) return;
        OpcodeStack.Item it = stack.getStackItem(stackDepth);
        Object value = it.getConstant();
        if (value == null || !(value instanceof String)) return;
        String regex = (String) value;
        if (!regex.equals(".")) return;
        if (ignorePasswordMasking) {
        	  OpcodeStack.Item top = stack.getStackItem(0);
        	  Object topValue = top.getConstant();
              if (topValue instanceof String) {
            	  String replacementString = (String) topValue;
            	  if (replacementString.length() == 1 &&  replacementString.toLowerCase().equals("x") || replacementString.equals("*") || replacementString.equals("\\*")) return;
              }
              
        }
        
        
       bugReporter.reportBug(new BugInstance(this, "RE_POSSIBLE_UNINTENDED_PATTERN", 
				NORMAL_PRIORITY)
                                .addClassAndMethod(this)
                                .addSourceLine(this)
				);
    }

    private void sawRegExPattern(int stackDepth) {
        sawRegExPattern(stackDepth, 0);
    }
    private void sawRegExPattern(int stackDepth, int flags) {
        if (stack.getStackDepth() < stackDepth) return;
        OpcodeStack.Item it = stack.getStackItem(stackDepth);
        Object value = it.getConstant();
        if (value == null || !(value instanceof String)) return;
        String regex = (String) value;
        try {
            Pattern.compile(regex, flags);
        } catch (PatternSyntaxException e) {
		  bugReporter.reportBug(new BugInstance(this, "RE_BAD_SYNTAX_FOR_REGULAR_EXPRESSION", 
				HIGH_PRIORITY)
                                .addClassAndMethod(this)
                                .addSourceLine(this)
				);
        }
    }

    /** return an int on the stack, or 'defaultValue' if can't determine */
    private int getIntValue(int stackDepth, int defaultValue) {
        if (stack.getStackDepth() < stackDepth) return defaultValue;
        OpcodeStack.Item it = stack.getStackItem(stackDepth);
        Object value = it.getConstant();
        if (value == null || !(value instanceof Integer)) return defaultValue;
        return ((Number)value).intValue();
    }

    @Override
    public void sawOpcode(int seen) {
    	stack.mergeJumps(this);
        if (seen == INVOKESTATIC 
            && getClassConstantOperand().equals("java/util/regex/Pattern")
            && getNameConstantOperand().equals("compile")
            && getSigConstantOperand().startsWith("(Ljava/lang/String;I)")
            ) 
            sawRegExPattern(1, getIntValue(0, 0));
        else if (seen == INVOKESTATIC 
            && getClassConstantOperand().equals("java/util/regex/Pattern")
            && getNameConstantOperand().equals("compile")
            && getSigConstantOperand().startsWith("(Ljava/lang/String;)")
            ) 
            sawRegExPattern(0);
        else if (seen == INVOKESTATIC 
            && getClassConstantOperand().equals("java/util/regex/Pattern")
            && getNameConstantOperand().equals("matches")
            ) 
            sawRegExPattern(1);
        else if (seen == INVOKEVIRTUAL 
            && getClassConstantOperand().equals("java/lang/String")
            && getNameConstantOperand().equals("replaceAll")
            ) {
            sawRegExPattern(1);
            singleDotPatternWouldBeSilly(1, true);
        	}
        else if (seen == INVOKEVIRTUAL 
            && getClassConstantOperand().equals("java/lang/String")
            && getNameConstantOperand().equals("replaceFirst")
            ) 
        {
            sawRegExPattern(1);
            singleDotPatternWouldBeSilly(1, false);
        	}
        else if (seen == INVOKEVIRTUAL 
            && getClassConstantOperand().equals("java/lang/String")
            && getNameConstantOperand().equals("matches")
            ) 
        {
            sawRegExPattern(0);
            singleDotPatternWouldBeSilly(0, false);
        	}
        else if (seen == INVOKEVIRTUAL 
            && getClassConstantOperand().equals("java/lang/String")
            && getNameConstantOperand().equals("split")
            ) 
        {
            sawRegExPattern(0);
            singleDotPatternWouldBeSilly(0, false);
        	}

        stack.sawOpcode(this,seen);
    }

}
