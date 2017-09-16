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
package edu.umd.cs.findbugs.plugin.eclipse.quickfix.util;

import static java.util.Objects.requireNonNull;
import static org.eclipse.core.runtime.Assert.isTrue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.CheckForNull;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.ASTNodeNotFoundException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.FieldDeclarationNotFoundException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.MethodDeclarationNotFoundException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.StatementNotFoundException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.TypeDeclarationNotFoundException;

/**
 * The <CODE>ASTUtil</CODE> provides some useful methods to transform
 * <CODE>PackageMemberAnnotations</CODE> into <CODE>BodyDeclarations</CODE>.
 * Normally this methods should be used to get a type, field or method
 * declaration for a class, field or method annotation.
 *
 * @see ASTUtil#getTypeDeclaration(CompilationUnit, ClassAnnotation)
 * @see ASTUtil#getFieldDeclaration(TypeDeclaration, FieldAnnotation)
 * @see ASTUtil#getMethodDeclaration(TypeDeclaration, MethodAnnotation)
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:g1zgragg@hsr.ch">Guido Zgraggen</a>
 * @version 1.0
 */
public class ASTUtil {

    private static Comparator<? super ImportDeclaration> defaultImportComparator;

    private static Map<String, Class<?>> primitiveTypes;

    static {
        defaultImportComparator = new ImportDeclarationComparator<>();

        primitiveTypes = new HashMap<>();
        primitiveTypes.put("B", byte.class);
        primitiveTypes.put("C", char.class);
        primitiveTypes.put("S", short.class);
        primitiveTypes.put("I", int.class);
        primitiveTypes.put("J", long.class);
        primitiveTypes.put("F", float.class);
        primitiveTypes.put("D", double.class);

    }

    public static void addImports(ASTRewrite rewrite, CompilationUnit compilationUnit, String... imports) {
        addImports(rewrite, compilationUnit, false, imports);
    }

    public static void addStaticImports(ASTRewrite rewrite, CompilationUnit compilationUnit, String... imports) {
        addImports(rewrite, compilationUnit, true, imports);
    }

    public static void addImports(ASTRewrite rewrite, CompilationUnit compilationUnit, boolean staticImports, String... imports) {
        addImports(rewrite, compilationUnit, defaultImportComparator, staticImports, imports);
    }

    public static void addImports(ASTRewrite rewrite, CompilationUnit compilationUnit,
            Comparator<? super ImportDeclaration> comparator, boolean staticImports, String... imports) {
        requireNonNull(comparator, "import comparator");
        requireNonNull(imports, "imports");

        final AST ast = rewrite.getAST();
        SortedSet<ImportDeclaration> importDeclarations = new TreeSet<>(comparator);
        for (String importName : imports) {
            ImportDeclaration importDeclaration = ast.newImportDeclaration();
            importDeclaration.setName(ast.newName(importName));
            importDeclaration.setStatic(staticImports);
            importDeclarations.add(importDeclaration);
        }
        addImports(rewrite, compilationUnit, importDeclarations);
    }

    /**
     * Adds <CODE>ImportDeclaration</CODE>s to the list of imports in the
     * specified <CODE>CompilationUnit</CODE>. If an import already exists, the
     * import will not be inserted. The imports are inserted in an ordered way.
     * The <CODE>Comparator</CODE> of the <CODE>SortedSet</CODE> is used to sort
     * the imports.
     *
     * @param rewrite
     *            the <CODE>ASTRewrite</CODE>, that stores the edits.
     * @param compilationUnit
     *            the <CODE>CompilationUnit</CODE>.
     * @param imports
     *            the new <CODE>ImportDeclaration</CODE>s to add.
     */
    public static void addImports(ASTRewrite rewrite, CompilationUnit compilationUnit, SortedSet<ImportDeclaration> imports) {
        requireNonNull(rewrite, "ast-rewrite");
        requireNonNull(compilationUnit, "compilation-unit");
        requireNonNull(imports, "imports");
        ListRewrite importRewrite = rewrite.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
        addImports(importRewrite, imports.comparator(), imports.iterator());
    }

    private static void addImports(ListRewrite importRewrite, Comparator<? super ImportDeclaration> comparator,
            Iterator<ImportDeclaration> newImports) {
        try {
            ImportDeclaration newImport = newImports.next();
            List<?> imports = importRewrite.getRewrittenList();
            for (Object importObj : imports) {
                ImportDeclaration anImport = (ImportDeclaration) importObj;
                int comp = comparator.compare(newImport, anImport);
                if (comp > 0) {
                    continue;
                }
                if (comp < 0) {
                    importRewrite.insertBefore(newImport, anImport, null);
                }
                newImport = newImports.next();
            }
            importRewrite.insertLast(newImport, null);
            while (newImports.hasNext()) {
                importRewrite.insertLast(newImports.next(), null);
            }
        } catch (NoSuchElementException e) {
            // do nothing
        }
    }

    public static ASTNode getASTNode(CompilationUnit compilationUnit, SourceLineAnnotation sourceLineAnno)
            throws ASTNodeNotFoundException {
        requireNonNull(sourceLineAnno, "source line annotation");
        return getASTNode(compilationUnit, sourceLineAnno.getStartLine(), sourceLineAnno.getEndLine());
    }

    /**
     * Searchs the first <CODE>ASTNode</CODE> between the specified
     * <CODE>startLine</CODE> and <CODE>endLine</CODE>. If the source line
     * doesn't contain an <CODE>ASTNode</CODE>, a
     * <CODE>ASTNodeNotFoundException</CODE> is thrown.
     *
     * @param compilationUnit
     *            the <CODE>CompilationUnit</CODE>, that contains the
     *            <CODE>ASTNode</CODE>.
     * @param startLine
     *            the starting source line number.
     * @param endLine
     *            the ending source line number.
     * @throws ASTNodeNotFoundException
     *             if no <CODE>ASTNode</CODE> found between the specifed start
     *             and end line.
     */
    public static ASTNode getASTNode(CompilationUnit compilationUnit, int startLine, int endLine) throws ASTNodeNotFoundException {
        requireNonNull(compilationUnit, "compilation unit");

        ASTNode node = searchASTNode(compilationUnit, startLine, endLine);
        if (node == null) {
            throw new ASTNodeNotFoundException("No ast node found between " + startLine + " and " + endLine + ".");
        }
        return node;
    }

    /**
     * Returns the <CODE>TypeDeclaration</CODE> for the specified
     * <CODE>ClassAnnotation</CODE>. The type has to be declared in the
     * specified <CODE>CompilationUnit</CODE>.
     *
     * @param compilationUnit
     *            The <CODE>CompilationUnit</CODE>, where the
     *            <CODE>TypeDeclaration</CODE> is declared in.
     * @param classAnno
     *            The <CODE>ClassAnnotation</CODE>, which contains the class
     *            name of the <CODE>TypeDeclaration</CODE>.
     * @return the <CODE>TypeDeclaration</CODE> found in the specified
     *         <CODE>CompilationUnit</CODE>.
     * @throws TypeDeclarationNotFoundException
     *             if no matching <CODE>TypeDeclaration</CODE> was found.
     */
    public static TypeDeclaration getTypeDeclaration(CompilationUnit compilationUnit, ClassAnnotation classAnno)
            throws TypeDeclarationNotFoundException {
        requireNonNull(classAnno, "class annotation");
        return getTypeDeclaration(compilationUnit, classAnno.getClassName());
    }

    /**
     * Returns the <CODE>TypeDeclaration</CODE> for the specified type name. The
     * type has to be declared in the specified <CODE>CompilationUnit</CODE>.
     *
     * @param compilationUnit
     *            The <CODE>CompilationUnit</CODE>, where the
     *            <CODE>TypeDeclaration</CODE> is declared in.
     * @param typeName
     *            The qualified class name to search for.
     * @return the <CODE>TypeDeclaration</CODE> found in the specified
     *         <CODE>CompilationUnit</CODE>.
     * @throws TypeDeclarationNotFoundException
     *             if no matching <CODE>TypeDeclaration</CODE> was found.
     */
    public static TypeDeclaration getTypeDeclaration(CompilationUnit compilationUnit, String typeName)
            throws TypeDeclarationNotFoundException {
        requireNonNull(compilationUnit, "compilation unit");
        requireNonNull(typeName, "class name");

        int index = typeName.lastIndexOf('.');
        String packageName = index > 0 ? typeName.substring(0, index) : "";
        if (!matchesPackage(compilationUnit.getPackage(), packageName)) {
            throw new TypeDeclarationNotFoundException(compilationUnit, typeName, "The package '" + packageName
                    + "' doesn't match the package of the compilation unit.");
        }

        TypeDeclaration type = searchTypeDeclaration(compilationUnit.types(), typeName.substring(index + 1));
        if (type == null) {
            throw new TypeDeclarationNotFoundException(compilationUnit, typeName);
        }
        return type;
    }

    /**
     * Returns the <CODE>FieldDeclaration</CODE> for the specified
     * <CODE>FieldAnnotation</CODE>. The field has to be declared in the
     * specified <CODE>TypeDeclaration</CODE>.
     *
     * @param type
     *            The <CODE>TypeDeclaration</CODE>, where the
     *            <CODE>FieldDeclaration</CODE> is declared in.
     * @param fieldAnno
     *            The <CODE>FieldAnnotation</CODE>, which contains the field
     *            name of the <CODE>FieldDeclaration</CODE>.
     * @return the <CODE>FieldDeclaration</CODE> found in the specified
     *         <CODE>TypeDeclaration</CODE>.
     * @throws FieldDeclarationNotFoundException
     *             if no matching <CODE>FieldDeclaration</CODE> was found.
     */
    public static FieldDeclaration getFieldDeclaration(TypeDeclaration type, FieldAnnotation fieldAnno)
            throws FieldDeclarationNotFoundException {
        requireNonNull(fieldAnno, "field annotation");

        return getFieldDeclaration(type, fieldAnno.getFieldName());
    }

    /**
     * Returns the <CODE>FieldDeclaration</CODE> for the specified field name.
     * The field has to be declared in the specified
     * <CODE>TypeDeclaration</CODE>.
     *
     * @param type
     *            The <CODE>TypeDeclaration</CODE>, where the
     *            <CODE>FieldDeclaration</CODE> is declared in.
     * @param fieldName
     *            The simple field name to search for.
     * @return the <CODE>FieldDeclaration</CODE> found in the specified
     *         <CODE>TypeDeclaration</CODE>.
     * @throws FieldDeclarationNotFoundException
     *             if no matching <CODE>FieldDeclaration</CODE> was found.
     */
    public static FieldDeclaration getFieldDeclaration(TypeDeclaration type, String fieldName)
            throws FieldDeclarationNotFoundException {
        requireNonNull(type, "type declaration");
        requireNonNull(fieldName, "field name");

        for (FieldDeclaration field : type.getFields()) {
            for (Object fragObj : field.fragments()) {
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragObj;
                if (fieldName.equals(fragment.getName().getIdentifier())) {
                    return field;
                }
            }
        }

        throw new FieldDeclarationNotFoundException(type, fieldName);
    }

    /**
     * Returns the <CODE>MethodDeclaration</CODE> for the specified
     * <CODE>MethodAnnotation</CODE>. The method has to be declared in the
     * specified <CODE>TypeDeclaration</CODE>.
     *
     * @param type
     *            The <CODE>TypeDeclaration</CODE>, where the
     *            <CODE>MethodDeclaration</CODE> is declared in.
     * @param methodAnno
     *            The <CODE>MethodAnnotation</CODE>, which contains the method
     *            name and signature of the <CODE>MethodDeclaration</CODE>
     * @return the <CODE>MethodDeclaration</CODE> found in the specified
     *         <CODE>TypeDeclaration</CODE>.
     * @throws MethodDeclarationNotFoundException
     *             if no matching <CODE>MethodDeclaration</CODE> was found.
     */
    public static MethodDeclaration getMethodDeclaration(TypeDeclaration type, MethodAnnotation methodAnno)
            throws MethodDeclarationNotFoundException {
        Objects.requireNonNull(methodAnno, "method annotation");

        return getMethodDeclaration(type, methodAnno.getMethodName(), methodAnno.getMethodSignature());
    }

    /**
     * Returns the <CODE>MethodDeclaration</CODE> for the specified method name
     * and signature. The method has to be declared in the specified
     * <CODE>TypeDeclaration</CODE>.
     *
     * @param type
     *            The <CODE>TypeDeclaration</CODE>, where the
     *            <CODE>MethodDeclaration</CODE> is declared in.
     * @param methodName
     *            The method name to search for.
     * @param methodSignature
     *            The method signature to search for.
     * @return the <CODE>MethodDeclaration</CODE> found in the specified
     *         <CODE>TypeDeclaration</CODE>.
     * @throws MethodDeclarationNotFoundException
     *             if no matching <CODE>MethodDeclaration</CODE> was found.
     */
    public static MethodDeclaration getMethodDeclaration(TypeDeclaration type, String methodName, String methodSignature)
            throws MethodDeclarationNotFoundException {
        requireNonNull(type, "type declaration");
        requireNonNull(methodName, "method name");
        requireNonNull(methodSignature, "method signature");

        MethodDeclaration method = searchMethodDeclaration(type.getAST(), type.getMethods(), methodName, methodSignature);
        if (method == null) {
            throw new MethodDeclarationNotFoundException(type, methodName, methodSignature);
        }
        return method;
    }

    public static Statement getStatement(CompilationUnit compilationUnit, MethodDeclaration method,
            SourceLineAnnotation sourceLineAnno) throws StatementNotFoundException {
        requireNonNull(sourceLineAnno, "source line annotation");

        return getStatement(compilationUnit, method, sourceLineAnno.getStartLine(), sourceLineAnno.getEndLine());
    }

    /**
     * Return the first <CODE>Statement</CODE> found, that is between the
     * specified start and end line.
     *
     * @param compilationUnit
     * @param method
     * @param startLine
     * @param endLine
     * @return
     * @throws StatementNotFoundException
     * @throws StatementNotFoundException
     */
    public static Statement getStatement(CompilationUnit compilationUnit, MethodDeclaration method, int startLine, int endLine)
            throws StatementNotFoundException {
        requireNonNull(compilationUnit, "compilation unit");
        requireNonNull(method, "method declaration");

        Statement statement = searchStatement(compilationUnit, method.getBody().statements(), startLine, endLine);
        if (statement == null) {
            throw new StatementNotFoundException(compilationUnit, startLine, endLine);
        }
        return statement;
    }

    @CheckForNull
    protected static ASTNode searchASTNode(CompilationUnit compilationUnit, int startLine, int endLine) {
        Assert.isNotNull(compilationUnit);
        isTrue(startLine <= endLine);

        SourceLineVisitor visitor = new SourceLineVisitor(compilationUnit, startLine, endLine);
        compilationUnit.accept(visitor);
        return visitor.getASTNode();
    }

    @CheckForNull
    protected static TypeDeclaration searchTypeDeclaration(List<?> declarations, String typeName) {
        Assert.isNotNull(declarations);
        Assert.isNotNull(typeName);

        int index = typeName.indexOf('$');
        String innerClassName = null;
        if (index >= 0) {
            innerClassName = typeName.substring(index + 1);
            typeName = typeName.substring(0, index);
        }

        for (Object declaration : declarations) {
            if (!(declaration instanceof TypeDeclaration)) {
                continue;
            }
            TypeDeclaration type = (TypeDeclaration) declaration;
            if (!typeName.equals(type.getName().getFullyQualifiedName())) {
                continue;
            }
            if (index < 0) {
                return type;
            }
            return searchTypeDeclaration(type.bodyDeclarations(), innerClassName);
        }
        return null;
    }

    @CheckForNull
    protected static MethodDeclaration searchMethodDeclaration(AST ast, MethodDeclaration[] methods, String methodName,
            String methodSignature) {
        Assert.isNotNull(methods);
        Assert.isNotNull(methodName);
        Assert.isNotNull(methodSignature);

        String[] parameters = parseParameters(methodSignature);
        for (MethodDeclaration method : methods) {
            if (!methodName.equals(method.getName().getFullyQualifiedName())) {
                continue;
            }
            if (!matchesParams(method.parameters(), parameters)) {
                continue;
            }
            return method;
        }
        return null;
    }

    @CheckForNull
    protected static Statement searchStatement(CompilationUnit compilationUnit, List<?> statements, int startLine, int endLine) {
        Assert.isNotNull(compilationUnit);
        Assert.isNotNull(statements);

        for (Object statementObj : statements) {
            Statement statement = (Statement) statementObj;
            int lineNumber = compilationUnit.getLineNumber(statement.getStartPosition());
            if (startLine <= lineNumber && lineNumber <= endLine) {
                return statement;
            }
        }
        return null;
    }

    protected static String[] parseParameters(String methodSignature) {
        Assert.isNotNull(methodSignature);

        int leftParenthesis = methodSignature.indexOf('(');
        int rightParenthesis = methodSignature.indexOf(')');
        methodSignature = methodSignature.substring(leftParenthesis + 1, rightParenthesis);

        if (methodSignature.length() == 0) {
            return new String[0];
        }

        String[] parameters = methodSignature.split(";");
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = normalizeParameter(parameters[i]);
        }
        return parameters;
    }

    protected static String normalizeParameter(String parameter) {
        Assert.isNotNull(parameter);

        Class<?> primitiveClass = primitiveTypes.get(parameter);
        if (primitiveClass != null) {
            return primitiveClass.getName();
        }

        if (parameter.startsWith("L")) {
            return parameter.substring(1).replaceAll("[/$]", ".");
        }
        if (parameter.startsWith("[")) {
            return normalizeParameter(parameter.substring(1)) + "[]";
        }

        throw new IllegalStateException("Unknown parameter type '" + parameter + "'.");
    }

    private static boolean matchesPackage(PackageDeclaration apackage, String packageName) {
        return apackage != null && packageName.equals(apackage.getName().getFullyQualifiedName()) || packageName.length() == 0;
    }

    private static boolean matchesParams(List<?> methodParams, String[] paramTypeNames) {
        return matchesParams(methodParams.toArray(new SingleVariableDeclaration[methodParams.size()]), paramTypeNames);
    }

    private static boolean matchesParams(SingleVariableDeclaration[] methodParams, String[] paramTypeNames) {
        if (methodParams.length != paramTypeNames.length) {
            return false;
        }
        for (int i = 0; i < methodParams.length; i++) {
            String typeName = getPrettyTypeName(methodParams[i].getType());
            if (!typeName.equals(paramTypeNames[i])) {
                return false;
            }
        }
        return true;
    }

    private static String getPrettyTypeName(Type type) {
        if (type.isArrayType()) {
            return getPrettyTypeName((ArrayType) type);
        } else if (type.isParameterizedType()) {
            return getPrettyTypeName((ParameterizedType) type);
        } else if (type.isPrimitiveType()) {
            return getPrettyTypeName((PrimitiveType) type);
        } else if (type.isQualifiedType()) {
            return getPrettyTypeName((QualifiedType) type);
        } else if (type.isSimpleType()) {
            return getPrettyTypeName((SimpleType) type);
        } else {
            return "";
        }
    }

    private static String getPrettyTypeName(ArrayType type) {
        return getPrettyTypeName(type.getComponentType()) + "[]";
    }

    private static String getPrettyTypeName(PrimitiveType type) {
        return type.getPrimitiveTypeCode().toString();
    }

    private static String getPrettyTypeName(ParameterizedType type) {
        String typeName = type.resolveBinding().getQualifiedName();
        return typeName.substring(0, typeName.indexOf('<'));
    }

    private static String getPrettyTypeName(QualifiedType type) {
        return type.resolveBinding().getQualifiedName();
    }

    private static String getPrettyTypeName(SimpleType type) {
        return type.resolveBinding().getQualifiedName();
    }

}
