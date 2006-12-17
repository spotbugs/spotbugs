/*
 * Contributions to FindBugs
 * Copyright (C) 2006, Institut for Software
 * An Institut of the University of Applied Sciences Rapperswil
 * 
 * Author: Thierry Wyss, Marco Busarello
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
import static org.eclipse.jdt.core.dom.ASTNode.VARIABLE_DECLARATION_STATEMENT;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * The <CODE>UseValueOfResolution</CODE> replace the inefficient creation of
 * an instance, by the static <CODE>valueOf(...)</CODE> method. 
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#DM_BOOLEAN_CTOR">DM_BOOLEAN_CTOR</a>
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#DM_FP_NUMBER_CTOR">DM_FP_NUMBER_CTOR</a>
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#DM_NUMBER_CTOR">DM_NUMBER_CTOR</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @version 1.0
 */
public class UseValueOfResolution extends BugResolution {

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        assert rewrite != null;
        assert workingUnit != null;

        TypeDeclaration type = getTypeDeclaration(workingUnit, bug.getPrimaryClass());
        MethodDeclaration method = getMethodDeclaration(type, bug.getPrimaryMethod());
        Statement statement = getStatement(workingUnit, method, bug.getPrimarySourceLineAnnotation());

        ClassInstanceCreation primitiveTypeCreation = findPrimitiveTypeCreation(statement);
        if (primitiveTypeCreation == null) {
            throw new BugResolutionException("Primitive type creation not found.");
        }

        MethodInvocation valueOfInvocation = createValueOfInvocation(rewrite, primitiveTypeCreation);

        rewrite.replace(primitiveTypeCreation, valueOfInvocation, null);
    }

    @CheckForNull
    protected ClassInstanceCreation findPrimitiveTypeCreation(Statement statement) {
        switch (statement.getNodeType()) {
            case VARIABLE_DECLARATION_STATEMENT:
                return findPrimitiveTypeCreation(((VariableDeclarationStatement) statement).fragments());
            default:
                return null;
        }
    }

    @CheckForNull
    protected ClassInstanceCreation findPrimitiveTypeCreation(List<VariableDeclarationFragment> variableDeclarations) {
        for (VariableDeclarationFragment variableDeclaration : variableDeclarations) {
            Expression initializer = variableDeclaration.getInitializer();
            if (initializer != null && initializer instanceof ClassInstanceCreation) {
                return (ClassInstanceCreation) initializer;
            }
        }
        return null;
    }

    protected MethodInvocation createValueOfInvocation(ASTRewrite rewrite, ClassInstanceCreation primitiveTypeCreation) {
        AST ast = rewrite.getAST();

        MethodInvocation valueOfInvocation = ast.newMethodInvocation();
        List<Expression> arguments = primitiveTypeCreation.arguments();
        SimpleName primitiveTypeName = ast.newSimpleName(primitiveTypeCreation.getType().resolveBinding().getName());

        valueOfInvocation.setExpression(primitiveTypeName);
        valueOfInvocation.setName(ast.newSimpleName("valueOf"));
        List<Expression> newArguments = valueOfInvocation.arguments();
        for (Expression argument : arguments) {
            Expression expression = (Expression) rewrite.createCopyTarget(argument);
            newArguments.add(expression);
        }

        return valueOfInvocation;
    }

    @Override
    protected boolean resolveBindings() {
        return true;
    }

}
