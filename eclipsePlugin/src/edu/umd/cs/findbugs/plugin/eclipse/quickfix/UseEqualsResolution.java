/*
 * Contributions to FindBugs
 * Copyright (C) 2004, Institut for Software
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
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.EQUALS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.NOT_EQUALS;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * Code that uses the == or != operators to compare Strings is bad code. The
 * <CODE>UseEqualsResolution</CODE> uses <CODE>equals()<CODE> instead.
 *
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#ES_COMPARING_PARAMETER_STRING_WITH_EQ">ES_COMPARING_PARAMETER_STRING_WITH_EQ</a>
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#ES_COMPARING_STRINGS_WITH_EQ">ES_COMPARING_STRINGS_WITH_EQ</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @version 1.0
 */
public class UseEqualsResolution extends BugResolution {

    private static final String EQUALS_METHOD_NAME = "equals";

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        Assert.isNotNull(rewrite);
        Assert.isNotNull(workingUnit);
        Assert.isNotNull(bug);

        InfixExpression[] stringEqualityChecks = findStringEqualityChecks(getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation()));
        for (InfixExpression stringEqualityCheck : stringEqualityChecks) {
            Operator operator = stringEqualityCheck.getOperator();
            Expression replaceExpression;
            if (EQUALS.equals(operator)) {
                replaceExpression = createEqualsExpression(rewrite, stringEqualityCheck);
            } else if (NOT_EQUALS.equals(operator)) {
                replaceExpression = createNotEqualsExpression(rewrite, stringEqualityCheck);
            } else {
                throw new BugResolutionException("Illegal operator '" + operator + "' found.");
            }
            rewrite.replace(stringEqualityCheck, replaceExpression, null);
        }
    }

    protected Expression createNotEqualsExpression(ASTRewrite rewrite, InfixExpression stringEqualityCheck) {
        Expression equalsExpression = createEqualsExpression(rewrite, stringEqualityCheck);

        final AST ast = rewrite.getAST();
        PrefixExpression prefixExpression = ast.newPrefixExpression();
        prefixExpression.setOperator(PrefixExpression.Operator.NOT);
        prefixExpression.setOperand(equalsExpression);
        return prefixExpression;
    }

    protected Expression createEqualsExpression(ASTRewrite rewrite, InfixExpression stringEqualityCheck) {
        Assert.isNotNull(rewrite);
        Assert.isNotNull(stringEqualityCheck);

        final AST ast = rewrite.getAST();
        MethodInvocation equalsInvocation = ast.newMethodInvocation();
        Expression leftOperand = createLeftOperand(rewrite, stringEqualityCheck.getLeftOperand());
        Expression rightOperand = createRightOperand(rewrite, stringEqualityCheck.getRightOperand());

        equalsInvocation.setName(ast.newSimpleName(EQUALS_METHOD_NAME));
        equalsInvocation.setExpression(leftOperand);

        ListRewrite argumentsRewrite = rewrite.getListRewrite(equalsInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
        argumentsRewrite.insertLast(rightOperand, null);

        return equalsInvocation;
    }

    private Expression createLeftOperand(ASTRewrite rewrite, Expression leftOperand) {
        Expression leftExp = (Expression) rewrite.createMoveTarget(leftOperand);
        if (leftOperand instanceof Name || leftOperand instanceof ParenthesizedExpression) {
            return leftExp;
        }
        final AST ast = rewrite.getAST();
        ParenthesizedExpression parExp = ast.newParenthesizedExpression();
        parExp.setExpression(leftExp);
        return parExp;
    }

    private Expression createRightOperand(ASTRewrite rewrite, Expression rightOperand) {
        if ((rightOperand instanceof ParenthesizedExpression)) {
            return createRightOperand(rewrite, ((ParenthesizedExpression) rightOperand).getExpression());
        }
        return (Expression) rewrite.createMoveTarget(rightOperand);
    }

    private InfixExpression[] findStringEqualityChecks(ASTNode node) {
        StringEqualityCheckFinder finder = new StringEqualityCheckFinder();
        node.accept(finder);
        return finder.getStringEqualityChecks();
    }

    static boolean isStringEqualityCheck(InfixExpression infix) {
        Operator op = infix.getOperator();
        return (EQUALS.equals(op) || NOT_EQUALS.equals(op)) && isStringOperand(infix.getLeftOperand()) && isStringOperand(infix.getRightOperand());
    }

    static boolean isStringOperand(Expression operand) {
        return !operand.resolveTypeBinding().isPrimitive();
    }

    static class StringEqualityCheckFinder extends ASTVisitor {

        private final Set<InfixExpression> objectEqualityChecks = new HashSet<InfixExpression>();

        @Override
        public boolean visit(InfixExpression node) {
            if (isStringEqualityCheck(node)) {
                objectEqualityChecks.add(node);
            }
            return true;
        }

        public InfixExpression[] getStringEqualityChecks() {
            return objectEqualityChecks.toArray(new InfixExpression[objectEqualityChecks.size()]);
        }

    }

}
