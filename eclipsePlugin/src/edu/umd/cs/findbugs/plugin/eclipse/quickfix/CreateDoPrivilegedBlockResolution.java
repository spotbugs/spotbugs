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

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.addImports;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;
import static java.util.Objects.requireNonNull;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.CheckForNull;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ImportDeclarationComparator;

/**
 * A <CODE>ClassLoader</CODE>, which requires a security manager, might be
 * invoked by code that does not have security permissions.In this case the
 * <CODE>ClassLoader</CODE> creation needs to occur inside a
 * <CODE>doPrivileged()</CODE>-Block. The class
 * <CODE>CreateDoPrivilegedBlockResolution</CODE> creates a new
 * <CODE>doPrivileged()</CODE>-Block around the <CODE>ClassLoader</CODE>
 * creation.
 *
 * @see <a
 *      href="http://findbugs.sourceforge.net/bugDescriptions.html#DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED">DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @version 1.0
 */
public class CreateDoPrivilegedBlockResolution extends BugResolution {

    private static final String DO_PRIVILEGED_METHOD_NAME = "doPrivileged"; //$NON-NLS-1$

    private boolean updateImports = true;

    private boolean staticImport = false;

    private Comparator<ImportDeclaration> importComparator = new ImportDeclarationComparator<ImportDeclaration>();

    public CreateDoPrivilegedBlockResolution() {
        super();
    }

    public CreateDoPrivilegedBlockResolution(boolean updateImports) {
        this();
        setUpdateImports(updateImports);
    }

    public CreateDoPrivilegedBlockResolution(boolean updateImports, boolean staticImport) {
        this(updateImports);
        setStaticImport(staticImport);
    }

    /**
     * Returns <CODE>true</CODE> if the imports were updated, otherwise
     * <CODE>false</CODE>.
     *
     * @return <CODE>true</CODE> or <CODE>false</CODE>. Default is
     *         <CODE>true</CODE>.
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

    /**
     * Returns <CODE>true</CODE> if the <CODE>doPrivileged()</CODE>-invocation
     * is imported statically. This feature should only be used under
     * source-level 1.5 or higher.
     *
     * @return <CODE>true</CODE> or </CODE>false</CODE>. Default is
     *         <CODE>false</CODE>.
     */
    public boolean isStaticImport() {
        return staticImport;
    }

    /**
     * Enables or disables static import for the <CODE>doPrivileged()</CODE>
     * -invocation. This feature should only be used under source-level 1.5 or
     * higher.
     *
     * @param staticImport
     *            the flag.
     */
    public void setStaticImport(boolean staticImport) {
        this.staticImport = staticImport;
    }

    public Comparator<ImportDeclaration> getImportComparator() {
        return importComparator;
    }

    public void setImportComparator(Comparator<ImportDeclaration> importComparator) {
        requireNonNull(importComparator, "import comparator");
        this.importComparator = importComparator;
    }

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        Assert.isNotNull(rewrite);
        Assert.isNotNull(workingUnit);
        Assert.isNotNull(bug);

        ClassInstanceCreation classLoaderCreation = findClassLoaderCreation(getASTNode(workingUnit,
                bug.getPrimarySourceLineAnnotation()));
        if (classLoaderCreation == null) {
            throw new BugResolutionException("No matching class loader creation found at the specified source line.");
        }
        updateVariableReferences(rewrite, classLoaderCreation);
        rewrite.replace(classLoaderCreation, createDoPrivilegedInvocation(rewrite, classLoaderCreation), null);
        updateImportDeclarations(rewrite, workingUnit);
    }

    protected void updateVariableReferences(ASTRewrite rewrite, ClassInstanceCreation classLoaderCreation) {
        Assert.isNotNull(rewrite);
        Assert.isNotNull(classLoaderCreation);

        MethodDeclaration method = findMethodDeclaration(classLoaderCreation);
        if (method != null) {
            final Set<String> variableReferences = findVariableReferences(classLoaderCreation.arguments());
            if (!variableReferences.isEmpty()) {
                updateMethodParams(rewrite, variableReferences, method.parameters());
                updateLocalVariableDeclarations(rewrite, variableReferences, method.getBody());
            }
        }
    }

    protected void updateMethodParams(ASTRewrite rewrite, Set<String> variables, List<?> params) {
        Assert.isNotNull(rewrite);
        Assert.isNotNull(variables);
        Assert.isNotNull(params);

        for (Object paramObj : params) {
            SingleVariableDeclaration param = (SingleVariableDeclaration) paramObj;
            if (variables.contains(param.getName().getFullyQualifiedName())) {
                ListRewrite listRewrite = rewrite.getListRewrite(param, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
                listRewrite.insertLast(rewrite.getAST().newModifier(ModifierKeyword.FINAL_KEYWORD), null);
            }
        }
    }

    protected void updateLocalVariableDeclarations(final ASTRewrite rewrite, final Set<String> variables, Block block) {
        Assert.isNotNull(rewrite);
        Assert.isNotNull(block);
        Assert.isNotNull(variables);

        final AST ast = rewrite.getAST();
        block.accept(new ASTVisitor() {

            @Override
            public boolean visit(VariableDeclarationFragment fragment) {
                if (variables.contains(fragment.getName().getFullyQualifiedName())) {
                    ASTNode parent = fragment.getParent();
                    if (parent instanceof VariableDeclarationStatement) {
                        ListRewrite listRewrite = rewrite
                                .getListRewrite(parent, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
                        listRewrite.insertLast(ast.newModifier(ModifierKeyword.FINAL_KEYWORD), null);
                    }
                }
                return true;
            }

        });

    }

    protected void updateImportDeclarations(ASTRewrite rewrite, CompilationUnit compilationUnit) {
        Assert.isNotNull(rewrite);
        Assert.isNotNull(compilationUnit);

        if (isUpdateImports()) {
            final AST ast = rewrite.getAST();
            SortedSet<ImportDeclaration> imports = new TreeSet<ImportDeclaration>(importComparator);
            imports.add(createImportDeclaration(ast, PrivilegedAction.class));
            if (!isStaticImport()) {
                imports.add(createImportDeclaration(ast, AccessController.class));
            } else {
                imports.add(createImportDeclaration(ast, AccessController.class, DO_PRIVILEGED_METHOD_NAME));
            }
            addImports(rewrite, compilationUnit, imports);
        }
    }

    protected ImportDeclaration createImportDeclaration(AST ast, Class<?> importClass) {
        Assert.isNotNull(ast);
        Assert.isNotNull(importClass);
        return createImportDeclaration(ast, importClass.getName(), false);
    }

    protected ImportDeclaration createImportDeclaration(AST ast, Class<?> importClass, String javaElementName) {
        Assert.isNotNull(ast);
        Assert.isNotNull(importClass);
        Assert.isNotNull(javaElementName);
        return createImportDeclaration(ast, importClass.getName() + "." + javaElementName, true);
    }

    private ImportDeclaration createImportDeclaration(AST ast, String name, boolean isStatic) {
        ImportDeclaration importDeclaration = ast.newImportDeclaration();
        importDeclaration.setName(ast.newName(name));
        importDeclaration.setStatic(isStatic);
        return importDeclaration;
    }

    protected MethodInvocation createDoPrivilegedInvocation(ASTRewrite rewrite, ClassInstanceCreation classLoaderCreation) {
        AST ast = rewrite.getAST();

        MethodInvocation doPrivilegedInvocation = ast.newMethodInvocation();
        ClassInstanceCreation privilegedActionCreation = createPrivilegedActionCreation(rewrite, classLoaderCreation);
        List<Expression> arguments = checkedList(doPrivilegedInvocation.arguments());

        if (!isStaticImport()) {
            Name accessControllerName;
            if (isUpdateImports()) {
                accessControllerName = ast.newSimpleName(AccessController.class.getSimpleName());
            } else {
                accessControllerName = ast.newName(AccessController.class.getName());
            }
            doPrivilegedInvocation.setExpression(accessControllerName);
        }
        doPrivilegedInvocation.setName(ast.newSimpleName(DO_PRIVILEGED_METHOD_NAME));
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
            privilegedActionName = ast.newSimpleName(PrivilegedAction.class.getSimpleName());
        } else {
            privilegedActionName = ast.newName(PrivilegedAction.class.getName());
        }
        SimpleType rawPrivilegedActionType = ast.newSimpleType(privilegedActionName);
        ParameterizedType privilegedActionType = ast.newParameterizedType(rawPrivilegedActionType);
        Type typeArgument = (Type) rewrite.createCopyTarget(classLoaderCreation.getType());
        List<Type> typeArguments = checkedList(privilegedActionType.typeArguments());

        typeArguments.add(typeArgument);

        return privilegedActionType;
    }

    @SuppressWarnings("unchecked")
    <V> List<V> checkedList(List<?> o) {
        return (List<V>) o;
    }

    private AnonymousClassDeclaration createAnonymousClassDeclaration(ASTRewrite rewrite,
            ClassInstanceCreation classLoaderCreation) {
        AST ast = rewrite.getAST();

        AnonymousClassDeclaration anonymousClassDeclaration = ast.newAnonymousClassDeclaration();
        MethodDeclaration runMethodDeclaration = createRunMethodDeclaration(rewrite, classLoaderCreation);
        List<BodyDeclaration> bodyDeclarations = checkedList(anonymousClassDeclaration.bodyDeclarations());

        bodyDeclarations.add(runMethodDeclaration);

        return anonymousClassDeclaration;
    }

    private MethodDeclaration createRunMethodDeclaration(ASTRewrite rewrite, ClassInstanceCreation classLoaderCreation) {
        AST ast = rewrite.getAST();

        MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        SimpleName methodName = ast.newSimpleName("run");
        Type returnType = (Type) rewrite.createCopyTarget(classLoaderCreation.getType());
        Block methodBody = createRunMethodBody(rewrite, classLoaderCreation);
        List<Modifier> modifiers = checkedList(methodDeclaration.modifiers());

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
        List<Statement> statements = checkedList(methodBody.statements());

        statements.add(returnStatement);
        returnStatement.setExpression((ClassInstanceCreation) rewrite.createCopyTarget(classLoaderCreation));

        return methodBody;
    }

    @CheckForNull
    private ClassInstanceCreation findClassLoaderCreation(ASTNode node) {
        ClassLoaderCreationFinder finder = new ClassLoaderCreationFinder();
        node.accept(finder);
        return finder.getClassLoaderCreation();
    }

    @CheckForNull
    private MethodDeclaration findMethodDeclaration(ASTNode node) {
        if (node == null || node instanceof MethodDeclaration) {
            return (MethodDeclaration) node;
        }
        return findMethodDeclaration(node.getParent());
    }

    private Set<String> findVariableReferences(List<?> arguments) {
        final Set<String> refs = new HashSet<String>();
        for (Object argumentObj : arguments) {
            Expression argument = (Expression) argumentObj;
            argument.accept(new ASTVisitor() {

                @Override
                public boolean visit(SimpleName node) {
                    if (!(node.getParent() instanceof Type)) {
                        refs.add(node.getFullyQualifiedName());
                    }
                    return true;
                }

            });
        }
        return refs;
    }

    protected static class ClassLoaderCreationFinder extends ASTVisitor {

        private ClassInstanceCreation classLoaderCreation;

        @Override
        public boolean visit(ClassInstanceCreation node) {
            if (classLoaderCreation == null) {
                if (!isClassLoaderCreation(node)) {
                    return true;
                }
                classLoaderCreation = node;
            }
            return false;
        }

        public ClassInstanceCreation getClassLoaderCreation() {
            return classLoaderCreation;
        }

        private static boolean isClassLoaderCreation(ClassInstanceCreation node) {
            return isClassLoader(node.getType());
        }

        private static boolean isClassLoader(Type type) {
            return isClassLoader(type.resolveBinding());
        }

        private static boolean isClassLoader(ITypeBinding typeBinding) {
            if (typeBinding.getQualifiedName().equals(ClassLoader.class.getName())) {
                return true;
            }

            ITypeBinding superclass = typeBinding.getSuperclass();
            return superclass != null && isClassLoader(superclass);
        }
    }

}
