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

package edu.umd.cs.findbugs.detect;
import edu.umd.cs.daveho.ba.ClassContext;
import edu.umd.cs.daveho.ba.SourceFile;
import edu.umd.cs.daveho.ba.SourceFinder;
import edu.umd.cs.findbugs.*;
import java.io.*;
import java.util.*;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;
import edu.umd.cs.pugh.visitclass.PreorderVisitor;

public class DroppedException extends PreorderVisitor implements Detector, Constants2 {
    private static final boolean DEBUG = Boolean.getBoolean("de.debug");
    private static final boolean IGNORE_COMMENTED_CATCH_BLOCKS = Boolean.getBoolean("de.comment");

    Set<String> reported = new HashSet<String>();
    Set<String> causes = new HashSet<String>();
    Set<String> checkedCauses = new HashSet<String>();
    private BugReporter bugReporter;

    public DroppedException(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
	}

    public void report() { }

    boolean isChecked(String c) {
	if (!causes.add(c)) return checkedCauses.contains(c);
	try {
		if (Repository.instanceOf(c, "java.lang.Exception")
			&& !Repository.instanceOf(c, "java.lang.RuntimeException"))
		   checkedCauses.add(c);
		return true;
	    }
	catch (ClassNotFoundException e) {
		bugReporter.reportMissingClass(e);
		}
	return false;
	}
		
		
    public void visit(Code obj) { 

	CodeException [] exp = obj.getExceptionTable();
	if (exp == null) return;
	byte [] code = obj.getCode();

	for(int i = 0; i < exp.length; i++)  {
	  int handled = exp[i].getHandlerPC();
	  int start = exp[i].getStartPC();
	  int end = exp[i].getEndPC();
	  int cause = exp[i].getCatchType();
	  boolean exitInTryBlock = false;

	  for(int j = start; j <= end;)   {
	    int opcode = asUnsignedByte(code[j]);
	    if (opcode >= IRETURN && opcode <= RETURN
		|| opcode >=IFEQ && opcode <= GOTO
				&& (opcode != GOTO || j < end)
			) {
			exitInTryBlock =  true;
			break;
			/*
			System.out.println("	exit: " + opcode 
				+ " in " + betterMethodName);
			*/
			}
		if (NO_OF_OPERANDS[opcode] < 0)  {
			exitInTryBlock = true;
			break;
			}
		j += 1+NO_OF_OPERANDS[opcode];
		}

	  if (exitInTryBlock) continue;
	  String c ;
	  if (cause == 0)
		c = "Throwable";
	  else
		{
		c = Utility.compactClassName(
		  constant_pool.getConstantString(cause, 
					CONSTANT_Class), false);
		if (!isChecked(c)) continue;
		}
	if (handled < 5) continue;
	/*
	if ( (0xff&code[handled]) == POP) {
		System.out.println( "DE:	" 
			+ betterMethodName
			+ " might ignore " + c
			+ " (" + (0xff&code[handled] )
			+ "," + (0xff&code[handled+1] )
			+")"
			);
		}
	else 
	*/
	int opcode = asUnsignedByte(code[handled]);
	 // System.out.println( "DE:	opcode is "  + opcode + ", " + asUnsignedByte(code[handled+1]));
	boolean drops = false;
	if ( opcode >= ASTORE_0
		&& opcode <= ASTORE_3
		 && asUnsignedByte(code[handled+1]) == RETURN) {
		if (DEBUG) System.out.println("Drop 1");
		drops = true;
		}
	if (handled+2 < code.length
		&&opcode == ASTORE
		 && asUnsignedByte(code[handled+2]) == RETURN) {
		drops = true;
		if (DEBUG) System.out.println("Drop 2");
		}
	if (handled+3 < code.length
	   && !exitInTryBlock) {
	if ( opcode >= ASTORE_0
		&& opcode <= ASTORE_3
		 && asUnsignedByte(code[handled-3]) == GOTO) {
		int offsetBefore = 
			asUnsignedByte(code[handled-2]) << 8
			| asUnsignedByte(code[handled-1]);
		if (offsetBefore == 4) {
			drops = true;
			if (DEBUG) System.out.println("Drop 3");
			}
		}
	if ( opcode == ASTORE
		 && asUnsignedByte(code[handled-3]) == GOTO) {
		int offsetBefore = 
			asUnsignedByte(code[handled-2]) << 8
			| asUnsignedByte(code[handled-1]);
		if (offsetBefore == 5) {
			drops = true;
			if (DEBUG) System.out.println("Drop 4");
			}
		}
	if ( opcode >= ASTORE_0
		&& opcode <= ASTORE_3
		 && asUnsignedByte(code[handled+1]) == GOTO 
		 && asUnsignedByte(code[handled-3]) == GOTO) {
		int offsetBefore = 
			asUnsignedByte(code[handled-2]) << 8
			| asUnsignedByte(code[handled-1]);
		int offsetAfter = 
			asUnsignedByte(code[handled+2]) << 8
			| asUnsignedByte(code[handled+3]);
	
		if (offsetAfter > 0 && offsetAfter+4 == offsetBefore)	  {
			drops = true;
			if (DEBUG) System.out.println("Drop 5");
			}
		}

	if ( opcode == ASTORE
		 && asUnsignedByte(code[handled+2]) == GOTO 
		 && asUnsignedByte(code[handled-3]) == GOTO) {
		int offsetBefore = 
			asUnsignedByte(code[handled-2]) << 8
			| asUnsignedByte(code[handled-1]);
		int offsetAfter = 
			asUnsignedByte(code[handled+3]) << 8
			| asUnsignedByte(code[handled+4]);
	
		if (offsetAfter > 0 && offsetAfter+5 == offsetBefore)	  {
			drops = true;
			if (DEBUG) System.out.println("Drop 6");
			}
		}




	}
	if (end-start >= 5 && drops && !c.equals("java.lang.InterruptedException")
			&& !c.equals("java.lang.CloneNotSupportedException")) {
	String key = (exitInTryBlock ? "mightDrop," : "mightIgnore,") + betterMethodName + "," + c;
	if (reported.add(key)) {
		BugInstance bugInstance = new BugInstance(exitInTryBlock ? "DE_MIGHT_DROP" : "DE_MIGHT_IGNORE", NORMAL_PRIORITY)
			.addClassAndMethod(this);

		SourceLineAnnotation srcLine = bugInstance.addSourceLine(this, handled).getPrimarySourceLineAnnotation();
		if (srcLine == null || !catchBlockHasComment(srcLine)) {
			bugInstance.addClass(c).describe("CLASS_EXCEPTION");
			bugReporter.reportBug(bugInstance);
		}
	}

	}
		}
}

  private static final int START = 0;
  private static final int CATCH = 1;
  private static final int OPEN_PAREN = 2;
  private static final int CLOSE_PAREN = 3;
  private static final int OPEN_BRACE = 4;

  /**
   * Maximum number of lines we look backwards to find the
   * "catch" keyword.  Looking backwards is necessary
   * when the indentation style puts the open brace on
   * a different line from the catch clause.
   */
  private static final int NUM_CONTEXT_LINES = 3;

  /**
   * The number of lines that we'll scan to look at the source
   * for a catch block.
   */
  private static final int MAX_LINES = 5;

  /**
   * Analyze a class's source code to see if there is a comment
   * (or other text) in a catch block we have marked as dropping
   * an exception.
   * @return true if there is a comment in the catch block,
   *   false if not (or if we can't tell)
   */
  private boolean catchBlockHasComment(SourceLineAnnotation srcLine) {
    if (!IGNORE_COMMENTED_CATCH_BLOCKS)
	return false;

    AnalysisContext analysisContext = AnalysisContext.instance();
    SourceFinder sourceFinder = analysisContext.getSourceFinder();
    try {
	SourceFile sourceFile = sourceFinder.findSourceFile(srcLine.getPackageName(), srcLine.getSourceFile());
	int startLine = srcLine.getStartLine();

	int scanStartLine = startLine - NUM_CONTEXT_LINES;
	if (scanStartLine < 1)
	    scanStartLine = 1;

	int offset = sourceFile.getLineOffset(scanStartLine - 1);
	if (offset < 0)
	    return false; // Source file has changed?
	Tokenizer tokenizer = new Tokenizer(new InputStreamReader(sourceFile.getInputStreamFromOffset(offset)));

	// Read the tokens into an ArrayList,
	// keeping track of where the catch block is reported
	// to start
	ArrayList<Token> tokenList = new ArrayList<Token>(40);
	int eolOfCatchBlockStart = -1;
	for (int line = scanStartLine; line < scanStartLine + MAX_LINES; ) {
	    Token token = tokenizer.next();
	    int kind = token.getKind();
	    if (kind == Token.EOF)
		break;

	    if (kind == Token.EOL) {
		if (line == startLine)
		    eolOfCatchBlockStart = tokenList.size();
		++line;
	    }

	    tokenList.add(token);
	}

	if (eolOfCatchBlockStart < 0)
	    return false; // Couldn't scan line reported as start of catch block

	// Starting at the end of the line reported as the start of the catch block,
	// scan backwards for the token "catch".
	ListIterator<Token> iter = tokenList.listIterator(eolOfCatchBlockStart);
	boolean foundCatch = false;

	while (iter.hasPrevious()) {
	    Token token = iter.previous();
	    if (token.getKind() == Token.WORD && token.getLexeme().equals("catch")) {
		foundCatch = true;
		break;
	    }
	}

	if (!foundCatch)
	    return false; // Couldn't find "catch" keyword

	// Scan forward from the "catch" keyword to see what text
	// is in the handler block.  If the block is non-empty,
	// then we suppress the warning (on the theory that the
	// programmer has indicated that there is a good reason
	// that the exception is ignored).
	boolean done = false;
	int numLines = 0;
	int state = START;
	int level = 0;
	do {
	    if (!iter.hasNext())
		break;

	    Token token = iter.next();
	    int type = token.getKind();
	    String value = token.getLexeme();

	    switch (type) {
	    case Token.EOL:
		if (DEBUG) System.out.println("Saw token: [EOL]");
		++numLines;
		if (numLines >= MAX_LINES)
		    done = true;
		break;
	    default:
		if (DEBUG) System.out.println("Got token: " + value);
		switch (state) {
		case START:
		    if (value.equals("catch"))
			state = CATCH;
		    break;
		case CATCH:
		    if (value.equals("("))
			state = OPEN_PAREN;
		    break;
		case OPEN_PAREN:
		    if (value.equals(")")) {
			if (level == 0)
			    state = CLOSE_PAREN;
			else
			    --level;
		    } else if (value.equals("(")) {
			++level;
		    }
		    break;
		case CLOSE_PAREN:
		    if (value.equals("{"))
			state = OPEN_BRACE;
		    break;
		case OPEN_BRACE:
		    boolean closeBrace = value.equals("}");
		    if (DEBUG && !closeBrace) System.out.println("Found a comment in catch block: " + value);
		    return !closeBrace;
		}
		break;
	    }
	} while (!done);
    } catch (IOException e) {
	// Ignored; we'll just assume there is no comment
	if (DEBUG) e.printStackTrace();
    }
    return false;
  }
}
