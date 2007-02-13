/*
 * Contributions to FindBugs
 * Copyright (C) 2006, Institut for Software
 * An Institut of the University of Applied Sciences Rapperswil
 * 
 * Author: Marco Busarello, Thierry Wyss
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
package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;
import static java.lang.Integer.parseInt;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.EQUALS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.REMAINDER;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * The code <CODE>x % 2 == 1</CODE> to check if a value is odd won't work for
 * negative numbers. The <CODE>CorrectOddnessCheckResolution</CODE> provides a
 * resolution to replace this bad check by an <CODE>expression</CODE> that
 * works also for negative numbers.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#IM_BAD_CHECK_FOR_ODD">IM_BAD_CHECK_FOR_ODD</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @version 1.0
 */
public abstract class CorrectOddnessCheckResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return false;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        assert rewrite != null;
        assert workingUnit != null;
        assert bug != null;

        InfixExpression oddnessCheck = findOddnessCheck(getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation()));
        if (oddnessCheck == null) {
            throw new BugResolutionException("No matching oddness check found at the specified source line.");
        }
        Expression numberExpression = findNumberExpression(oddnessCheck);
        if (numberExpression == null) {
            throw new BugResolutionException();
        }
        InfixExpression correctOddnessCheck = createCorrectOddnessCheck(rewrite, numberExpression);
        rewrite.replace(oddnessCheck, correctOddnessCheck, null);
    }

    @CheckForNull
    protected InfixExpression findOddnessCheck(ASTNode node) {
        OddnessCheckFinder finder = new OddnessCheckFinder();
        node.accept(finder);
        return finder.getOddnessCheck();
    }

    @CheckForNull
    protected Expression findNumberExpression(InfixExpression oddnessCheck) {
        NumberExpressionFinder finder = new NumberExpressionFinder();
        oddnessCheck.accept(finder);
        return finder.getNumberExpression();
    }

    /**
     * Creates and returns a correct <CODE>expression</CODE> that checks if a
     * value is odd or not.
     * 
     * @param ast
     *            the <CODE>AST</CODE> instance under which the created <CODE>InfixExpression</CODE>
     *            will be created.
     * @param replaceField
     *            the field name of the bad oddness-check which will be used for
     *            the new <CODE>InfixExpression</CODE>.
     * @return the correct <CODE>InfixExpression</CODE>.
     */
    protected abstract InfixExpression createCorrectOddnessCheck(ASTRewrite rewrite, Expression numberExpression);

    protected static boolean isOddnessCheck(InfixExpression oddnessCheck) {
        if (EQUALS.equals(oddnessCheck.getOperator())) {
            if (isRemainderExp(oddnessCheck.getLeftOperand())) {
                return isNumber(oddnessCheck.getRightOperand(), 1);
            }
            if (isRemainderExp(oddnessCheck.getRightOperand())) {
                return isNumber(oddnessCheck.getLeftOperand(), 1);
            }
        }
        return false;
    }

    protected static boolean isRemainderExp(Expression remainderExp) {
        while (remainderExp instanceof ParenthesizedExpression) {
            remainderExp = ((ParenthesizedExpression) remainderExp).getExpression();
        }
        if (remainderExp instanceof InfixExpression) {
            InfixExpression exp = ((InfixExpression) remainderExp);
            return REMAINDER.equals(exp.getOperator()) && isNumber(exp.getRightOperand(), 2);
        }
        return false;
    }

    protected static boolean isNumber(Expression exp, int number) {
        return exp instanceof NumberLiteral && parseInt(((NumberLiteral) exp).getToken()) == number;
    }

    protected static class OddnessCheckFinder extends ASTVisitor {

        private InfixExpression oddnessCheck = null;

        @Override
        public boolean visit(InfixExpression node) {
            if (oddnessCheck == null) {
                if (!isOddnessCheck(node)) {
                    return true;
                }
                oddnessCheck = node;
            }
            return false;
        }

        public InfixExpression getOddnessCheck() {
            return oddnessCheck;
        }

    }

    protected static class NumberExpressionFinder extends ASTVisitor {

        private Expression numberExpression = null;

        @Override
        public boolean visit(InfixExpression node) {
            if (numberExpression == null) {
                if (!isRemainderExp(node)) {
                    return true;
                }
                numberExpression = node.getLeftOperand();
            }
            return false;
        }

        public Expression getNumberExpression() {
            return numberExpression;
        }

    }

}
