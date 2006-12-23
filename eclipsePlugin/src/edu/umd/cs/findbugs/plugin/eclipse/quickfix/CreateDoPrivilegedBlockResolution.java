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
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.TigerSubstitutes;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * A <CODE>ClassLoader</CODE>, which requires a security manager, might be
 * invoked by code that does not have security permissions.In this case the
 * <CODE>ClassLoader</CODE> creation needs to occur inside a <CODE>doPrivileged()</CODE>-Block.
 * The class <CODE>CreateDoPrivilegedBlockResolution</CODE> creates a new
 * <CODE>doPrivileged()</CODE>-Block around the <CODE>ClassLoader</CODE>
 * creation.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED">DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @version 1.0
 */
public class CreateDoPrivilegedBlockResolution extends BugResolution {

    private boolean updateImports = true;

    /**
     * Returns <CODE>true</CODE> if the imports were updated, otherwise <CODE>false</CODE>.
     * 
     * @return <CODE>true</CODE> or <CODE>false</CODE>.
     */
    public boolean isUpdateImports() {
        return updateImports;
    }

    /**
     * Enables or disables the update on the imports.
     * 
     * @param updateImports
     *            the flag.
     */
    public void setUpdateImports(boolean updateImports) {
        this.updateImports = updateImports;
    }

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        assert rewrite != null;
        assert workingUnit != null;
        assert bug != null;

        TypeDeclaration type = getTypeDeclaration(workingUnit, bug.getPrimaryClass());
        MethodDeclaration method = getMethodDeclaration(type, bug.getPrimaryMethod());
        Statement statement = getStatement(workingUnit, method, bug.getPrimarySourceLineAnnotation());
        ClassInstanceCreation classLoaderCreation = findClassLoaderCreation(statement);

        if (classLoaderCreation == null) {
            throw new BugResolutionException("No matching class loader creation found at the specified source line.");
        }

        MethodInvocation doPrivilegedInvocation = createDoPrivilegedInvocation(rewrite, classLoaderCreation);
        rewrite.replace(classLoaderCreation, doPrivilegedInvocation, null);

        if (isUpdateImports()) {
            updateImportDeclarations(rewrite, workingUnit);
        }
    }

    protected void updateImportDeclarations(ASTRewrite rewrite, CompilationUnit compilationUnit) {
        assert rewrite != null;
        assert compilationUnit != null;

        ListRewrite importRewrite = rewrite.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
        updateImportDeclaration(importRewrite, AccessController.class);
        updateImportDeclaration(importRewrite, PrivilegedAction.class);
    }

    protected void updateImportDeclaration(ListRewrite importRewrite, final Class<?> importType) {
        assert importRewrite != null;
        assert importType != null;

        AST ast = importRewrite.getASTRewrite().getAST();
        ImportDeclaration importDeclaration = createImportDeclaration(ast, importType);
        List<ImportDeclaration> imports = importRewrite.getRewrittenList();
        int index = searchImportPosition(imports, importDeclaration);
        if (index >= 0 && index <= imports.size()) {
            importRewrite.insertAt(importDeclaration, index, null);
        }
    }

    private int searchImportPosition(List<ImportDeclaration> imports, ImportDeclaration importDeclaration) {
        String fullyQualifiedName = importDeclaration.getName().getFullyQualifiedName();
        int index = 0;
        for (ImportDeclaration impDec : imports) {
            String fqn = impDec.getName().getFullyQualifiedName();
            if (fqn.equals(fullyQualifiedName)) {
                return -1;
            }
            if (fqn.compareTo(fullyQualifiedName) < 0) {
                index++;
            }
        }
        return index;
    }

    protected ImportDeclaration createImportDeclaration(AST ast, Class<?> importType) {
        ImportDeclaration importDeclaration = ast.newImportDeclaration();

        importDeclaration.setName(ast.newName(importType.getName()));

        return importDeclaration;
    }

    protected MethodInvocation createDoPrivilegedInvocation(ASTRewrite rewrite, ClassInstanceCreation classLoaderCreation) {
        AST ast = rewrite.getAST();

        MethodInvocation doPrivilegedInvocation = ast.newMethodInvocation();
        ClassInstanceCreation privilegedActionCreation = createPrivilegedActionCreation(rewrite, classLoaderCreation);
        List<Expression> arguments = doPrivilegedInvocation.arguments();

        Name accessControllerName;
        if (isUpdateImports()) {
            accessControllerName = ast.newSimpleName(TigerSubstitutes.getSimpleName(AccessController.class));
        } else {
            accessControllerName = ast.newName(AccessController.class.getName());
        }
        doPrivilegedInvocation.setExpression(accessControllerName);
        doPrivilegedInvocation.setName(ast.newSimpleName("doPrivileged")); //$NON-NLS-1$
        arguments.add(privilegedActionCreation);

        return doPrivilegedInvocation;
    }

    private ClassInstanceCreation createPrivilegedActionCreation(ASTRewrite rewrite, ClassInstanceCreation classLoaderCreation) {
        AST ast = rewrite.getAST();

        ClassInstanceCreation privilegedActionCreation = ast.newClassInstanceCreation();
        ParameterizedType privilegedActionType = createPrivilegedActionType(rewrite, classLoaderCreation);
        AnonymousClassDeclaration anonymousClassDeclaration = createAnonymousClassDeclaration(rewrite, classLoaderCreation);

        privilegedActionCreation.setType(privilegedActionType);
        privilegedActionCreation.setAnonymousClassDeclaration(anonymousClassDeclaration);

        return privilegedActionCreation;
    }

    private ParameterizedType createPrivilegedActionType(ASTRewrite rewrite, ClassInstanceCreation classLoaderCreation) {
        AST ast = rewrite.getAST();

        Name privilegedActionName;
        if (isUpdateImports()) {
            privilegedActionName = ast.newSimpleName(TigerSubstitutes.getSimpleName(PrivilegedAction.class));
        } else {
            privilegedActionName = ast.newName(PrivilegedAction.class.getName());
        }
        SimpleType rawPrivilegedActionType = ast.newSimpleType(privilegedActionName);
        ParameterizedType privilegedActionType = ast.newParameterizedType(rawPrivilegedActionType);
        Type typeArgument = (Type) rewrite.createCopyTarget(classLoaderCreation.getType());
        List<Type> typeArguments = privilegedActionType.typeArguments();

        typeArguments.add(typeArgument);

        return privilegedActionType;
    }

    private AnonymousClassDeclaration createAnonymousClassDeclaration(ASTRewrite rewrite, ClassInstanceCreation classLoaderCreation) {
        AST ast = rewrite.getAST();

        AnonymousClassDeclaration anonymousClassDeclaration = ast.newAnonymousClassDeclaration();
        MethodDeclaration runMethodDeclaration = createRunMethodDeclaration(rewrite, classLoaderCreation);
        List<BodyDeclaration> bodyDeclarations = anonymousClassDeclaration.bodyDeclarations();

        bodyDeclarations.add(runMethodDeclaration);

        return anonymousClassDeclaration;
    }

    private MethodDeclaration createRunMethodDeclaration(ASTRewrite rewrite, ClassInstanceCreation classLoaderCreation) {
        AST ast = rewrite.getAST();

        MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        SimpleName methodName = ast.newSimpleName("run");
        Type returnType = (Type) rewrite.createCopyTarget(classLoaderCreation.getType());
        Block methodBody = createRunMethodBody(rewrite, classLoaderCreation);
        List<Modifier> modifiers = methodDeclaration.modifiers();

        modifiers.add(ast.newModifier(PUBLIC_KEYWORD));
        methodDeclaration.setName(methodName);
        methodDeclaration.setReturnType2(returnType);
        methodDeclaration.setBody(methodBody);

        return methodDeclaration;
    }

    private Block createRunMethodBody(ASTRewrite rewrite, ClassInstanceCreation classLoaderCreation) {
        AST ast = rewrite.getAST();

        Block methodBody = ast.newBlock();
        ReturnStatement returnStatement = ast.newReturnStatement();
        List<Statement> statements = methodBody.statements();

        statements.add(returnStatement);
        returnStatement.setExpression((ClassInstanceCreation) rewrite.createCopyTarget(classLoaderCreation));

        return methodBody;
    }

    @CheckForNull
    protected ClassInstanceCreation findClassLoaderCreation(Statement statement) {
        assert statement != null;

        switch (statement.getNodeType()) {
            case ASTNode.VARIABLE_DECLARATION_STATEMENT:
                if (isClassLoader(((VariableDeclarationStatement) statement).getType())) {
                    List<VariableDeclarationFragment> fragments = ((VariableDeclarationStatement) statement).fragments();
                    for (VariableDeclarationFragment fragment : fragments) {
                        Expression exp = fragment.getInitializer();
                        ClassInstanceCreation creation = findClassLoaderCreation(exp);
                        if (creation != null) {
                            return creation;
                        }
                    }
                }
                return null;
            case ASTNode.EXPRESSION_STATEMENT:
                return findClassLoaderCreation(((ExpressionStatement) statement).getExpression());
            default:
                return null;
        }
    }

    @CheckForNull
    private ClassInstanceCreation findClassLoaderCreation(Expression expression) {
        switch (expression.getNodeType()) {
            case ASTNode.ASSIGNMENT:
                return findClassLoaderCreation(((Assignment) expression).getRightHandSide());
            case ASTNode.CLASS_INSTANCE_CREATION:
                if (isClassLoaderCreation((ClassInstanceCreation) expression)) {
                    return (ClassInstanceCreation) expression;
                }
            default:
                return null;
        }
    }

    private boolean isClassLoaderCreation(ClassInstanceCreation classInstanceCreation) {
        return isClassLoader(classInstanceCreation.getType());
    }

    private boolean isClassLoader(Type type) {
        return isClassLoader(type.resolveBinding());
    }

    private boolean isClassLoader(ITypeBinding typeBinding) {
        if (typeBinding.getQualifiedName().equals(ClassLoader.class.getName())) {
            return true;
        }

        ITypeBinding superclass = typeBinding.getSuperclass();
        return superclass != null && isClassLoader(superclass);
    }

}
