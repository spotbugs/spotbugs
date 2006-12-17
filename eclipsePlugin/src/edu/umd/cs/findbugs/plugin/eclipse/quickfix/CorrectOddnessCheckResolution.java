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

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getMethodDeclaration;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getStatement;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getTypeDeclaration;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import edu.umd.cs.findbugs.BugInstance;
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
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        assert rewrite != null;
        assert workingUnit != null;
        assert bug != null;

        TypeDeclaration type = getTypeDeclaration(workingUnit, bug.getPrimaryClass());
        MethodDeclaration method = getMethodDeclaration(type, bug.getPrimaryMethod());
        Statement statement = getStatement(workingUnit, method, bug.getPrimarySourceLineAnnotation());
        String originalFieldName = null;
        if (statement instanceof IfStatement) {
            originalFieldName = getOriginalFieldName((IfStatement) statement);
        } else {
            throw new BugResolutionException("Statement is not of type IfStatement");
        }

        AST ast = workingUnit.getAST();
        SimpleName replaceField = ast.newSimpleName(originalFieldName);

        InfixExpression replaceExpression = createReplaceExpression(ast, replaceField);

        rewrite.set(statement, IfStatement.EXPRESSION_PROPERTY, replaceExpression, null);
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
    protected abstract InfixExpression createReplaceExpression(AST ast, SimpleName replaceField);

    protected String getOriginalFieldName(IfStatement originalStatement) {
        assert originalStatement != null;
        InfixExpression originalLeftOperand;
        InfixExpression originalExpression = (InfixExpression) originalStatement.getExpression();
        Expression leftOperand = originalExpression.getLeftOperand();
        if (leftOperand.getClass().equals(ParenthesizedExpression.class)) {
            ParenthesizedExpression parEx = (ParenthesizedExpression) originalExpression.getLeftOperand();
            originalLeftOperand = (InfixExpression) parEx.getExpression();
        } else {
            originalLeftOperand = (InfixExpression) originalExpression.getLeftOperand();
        }
        SimpleName originalField = (SimpleName) originalLeftOperand.getLeftOperand();
        return originalField.getIdentifier();
    }

    @Override
    protected boolean resolveBindings() {
        return true;
    }

}
