/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.IntAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import org.apache.bcel.classfile.Code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatStringChecker extends OpcodeStackDetector {
	private static final boolean VAMISMATCH_DEBUG = SystemProperties.getBoolean("vamismatch.debug");

	BugReporter bugReporter;

	public FormatStringChecker(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	enum FormatState {
		NONE, READY_FOR_FORMAT, EXPECTING_ASSIGNMENT
	};

	FormatState state;

	String formatString;

	int stackDepth;

	OpcodeStack.Item arguments[];

	@Override
	public void visit(Code code) {
		boolean interesting = true;
		state = FormatState.NONE;
		if (interesting) {
			// initialize any variables we want to initialize for the method
			super.visit(code); // make callbacks to sawOpcode for all opcodes
		}
		arguments = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
	 */
	@Override
	public void sawOpcode(int seen) {

		if (stack.getStackDepth() < stackDepth) {
			state = FormatState.NONE;
			stackDepth = 0;
			arguments = null;
		}
		if (seen == ANEWARRAY && stack.getStackDepth() >= 2) {
			Object size = stack.getStackItem(0).getConstant();
			Object formatString = stack.getStackItem(1).getConstant();
			if (size instanceof Integer && formatString instanceof String) {
				arguments = new OpcodeStack.Item[(Integer) size];
				this.formatString = (String) formatString;
				state = FormatState.READY_FOR_FORMAT;
				stackDepth = stack.getStackDepth();
			}
		} else if (state == FormatState.READY_FOR_FORMAT && seen == DUP)
			state = FormatState.EXPECTING_ASSIGNMENT;
		else if (state == FormatState.EXPECTING_ASSIGNMENT && stack.getStackDepth() == stackDepth + 3 && seen == AASTORE) {
			Object pos = stack.getStackItem(1).getConstant();
			OpcodeStack.Item value = stack.getStackItem(0);
			if (pos instanceof Integer) {
				int index = (Integer) pos;
				if (index >= 0 && index < arguments.length) {
					arguments[index] = value;
					state = FormatState.READY_FOR_FORMAT;
				} else
					state = FormatState.NONE;
			} else
				state = FormatState.NONE;
		}  else if (state == FormatState.READY_FOR_FORMAT
		        && (seen == INVOKESPECIAL || seen == INVOKEVIRTUAL || seen == INVOKESTATIC || seen == INVOKEINTERFACE)
		        && stack.getStackDepth() == stackDepth) {
			String cl = getClassConstantOperand();
			String nm = getNameConstantOperand();
			if ("java/util/Formatter".equals(cl) && "format".equals(nm) || "java/lang/String".equals(cl) && "format".equals(nm)
			        || "java/io/PrintStream".equals(cl) && "format".equals(nm) || "java/io/PrintStream".equals(cl)
			        && "printf".equals(nm) || "java/io/PrintWriter".equals(cl) && "format".equals(nm)
			        || "java/io/PrintWriter".equals(cl) && "printf".equals(nm)) {

				try {
					FormatSpecifier[] formats = parse(formatString);
	                check(formats, arguments);
                } catch (IllegalArgumentException e) {
                	bugReporter.reportBug(
							new BugInstance(this, "VA_FORMAT_STRING_ILLEGAL", HIGH_PRIORITY)
							.addClassAndMethod(this)
							.addCalledMethod(this)
							.addString(formatString)
							.addSourceLine(this)
						);
                } catch (MissingFormatArgumentException e) {
                
                	bugReporter.reportBug(
							new BugInstance(this, "VA_FORMAT_STRING_MISSING_ARGUMENT", HIGH_PRIORITY)
							.addClassAndMethod(this)
							.addCalledMethod(this)
							.addString(formatString)
							.addString(e.formatSpecifier.toString())
							.addInt(e.pos+1)
							.addInt(arguments.length).describe(IntAnnotation.INT_ACTUAL_ARGUMENTS)
							.addSourceLine(this)
						);
                } catch (ExtraFormatArgumentsException e) {
                	bugReporter.reportBug(
							new BugInstance(this, "VA_FORMAT_STRING_EXTRA_ARGUMENTS_PASSED", NORMAL_PRIORITY)
							.addClassAndMethod(this)
							.addCalledMethod(this)
							.addString(formatString)
							.addInt(e.used).describe(IntAnnotation.INT_EXPECTED_ARGUMENTS)
							.addInt(e.provided).describe(IntAnnotation.INT_ACTUAL_ARGUMENTS)
							.addSourceLine(this)
						);
                }
			}

		}
	}
	
    // %[argument_index$][flags][width][.precision][t]conversion
    private static final String formatSpecifier
        = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";

    private static Pattern fsPattern = Pattern.compile(formatSpecifier);


    static class FormatSpecifier{
    	 private final String fullPattern;
    	 private final int index;
         private final String flags;
         private final int width;
         private final int precision;
         private final boolean dt;
         private final char c;

    	static int parseWidth(String n) {
    		if (n == null) return -1;
    		int value = Integer.parseInt(n);
    		if (value < 0) throw new IllegalArgumentException("Illegal value: " + value);
			return value;
    	}

    	@Override
        public String toString() {
    		return fullPattern;
    	}
        public FormatSpecifier(String fullPattern, String[] sa) {
        	this.fullPattern = fullPattern;
        	int idx = 0;
        	String indexString = sa[idx++];
        	
        	flags = sa[idx++];
            width = parseWidth(sa[idx++]);
            String precisionWidthString = sa[idx++];
            if (precisionWidthString == null)
            	precision = -1;
            else precision = parseWidth(precisionWidthString.substring(1));
            

            if (sa[idx++] != null) {
                dt = true;
         
            } else
            	dt = false;
            c = sa[idx++].charAt(0);
            
            if (c == '%' || c == 'n')
            	index = -2;
            else if (flags.indexOf('<') >= 0)
            	index = -1;
            else if (indexString == null)
        		index = 0;
        	else index = Integer.parseInt(indexString.substring(0, indexString.length()-1));
        	

        }};
    
    private FormatSpecifier[] parse(String s) {
        ArrayList<FormatSpecifier> al = new ArrayList<FormatSpecifier>();
        Matcher m = fsPattern.matcher(s);
        int i = 0;
        while (i < s.length()) {
            if (m.find(i)) {
                // Anything between the start of the string and the beginning
                // of the format specifier is either fixed text or contains
                // an invalid format string.
                if (m.start() != i) {
                    // Make sure we didn't miss any invalid format specifiers
                    checkText(s.substring(i, m.start()));
                    // Assume previous characters were fixed text
                    // ignore them
                }

                // Expect 6 groups in regular expression
                String[] sa = new String[6];
                for (int j = 0; j < m.groupCount(); j++)
                    {
                    sa[j] = m.group(j + 1);
                    }
                al.add(new FormatSpecifier(m.group(0), sa));
                i = m.end();
            } else {
                // No more valid format specifiers.  Check for possible invalid
                // format specifiers.
                checkText(s.substring(i));
                // The rest of the string is fixed text, ignore it
                break;
            }
        }
        return (FormatSpecifier[]) al.toArray(new FormatSpecifier[0]);
    }
    public void check(FormatSpecifier[] fsa, OpcodeStack.Item [] args) throws MissingFormatArgumentException, ExtraFormatArgumentsException {
 
        // index of last argument referenced
        int last = -1;
        // last ordinary index
        int lasto = -1;
        
        int maxIndex = -1;

        for (int i = 0; i < fsa.length; i++) {
        	FormatSpecifier fs = fsa[i];
            int index = fs.index;

                switch (index) {
                case -2:  // fixed string, "%n", or "%%"
                    // ignore
                    break;
                case -1:  // relative index
                    if (last < 0 || (last > args.length - 1))
                        throw new MissingFormatArgumentException(last, fs);
                    // check fs against args[last]
                    maxIndex = Math.max(maxIndex, last);
                    break;
                case 0:  // ordinary index
                    lasto++;
                    last = lasto;
                    if (lasto > args.length - 1)
                        throw new MissingFormatArgumentException(lasto, fs);
                    // check fs against args[lasto]
                    maxIndex = Math.max(maxIndex, lasto);
                    break;
                default:  // explicit index
                    last = index - 1;
                    if (last > args.length - 1)
                        throw new MissingFormatArgumentException(last, fs);
                    // check fs against args[last]
                    maxIndex = Math.max(maxIndex, last);
                    break;
                }
          
        }
        if (maxIndex < args.length -1)
        	throw new ExtraFormatArgumentsException(args.length, maxIndex+1);

    }

    
    
    private void checkText(String s) {
        int idx;
        // If there are any '%' in the given string, we got a bad format
        // specifier.
        if ((idx = s.indexOf('%')) != -1) {
            char c = (idx > s.length() - 2 ? '%' : s.charAt(idx + 1));
            throw new IllegalArgumentException("Unknown format string specified: " + String.valueOf(c));
        }
    }


    class MissingFormatArgumentException extends Exception {
    	final int pos;
    	final FormatSpecifier formatSpecifier;
    	MissingFormatArgumentException(int pos, FormatSpecifier formatSpecifier) {
    		this.pos = pos;
    		this.formatSpecifier = formatSpecifier;
    	}
    }
    
    class ExtraFormatArgumentsException extends Exception {
    	final int provided;
    	final int used;
    	ExtraFormatArgumentsException(int provided, int used) {
    		this.provided = provided;
    		this.used = used;
    	}
    }

	

}
