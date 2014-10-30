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

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.addStaticImports;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * The <CODE>UseValueOfResolution</CODE> replace the inefficient creation of an
 * instance, by the static <CODE>valueOf(...)</CODE> method.
 *
 * @see <a
 *      href="http://findbugs.sourceforge.net/bugDescriptions.html#DM_BOOLEAN_CTOR">DM_BOOLEAN_CTOR</a>
 * @see <a
 *      href="http://findbugs.sourceforge.net/bugDescriptions.html#DM_FP_NUMBER_CTOR">DM_FP_NUMBER_CTOR</a>
 * @see <a
 *      href="http://findbugs.sourceforge.net/bugDescriptions.html#DM_NUMBER_CTOR">DM_NUMBER_CTOR</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @version 1.0
 */
public class UseValueOfResolution extends BugResolution {

    private static final String VALUE_OF_METHOD_NAME = "valueOf";

    private static final Set<String> primitiveWrapperClasses = new HashSet<>();

    static {
        primitiveWrapperClasses.add("java.lang.Double");
        primitiveWrapperClasses.add("java.lang.Integer");
        primitiveWrapperClasses.add("java.lang.Boolean");
        primitiveWrapperClasses.add("java.lang.Float");
    }

    private boolean isDouble;

    private boolean isFloatingPoint;

    public boolean isStaticImport() {
        // This can be changed to true to add the static import and base the fix on that.
        return false;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        Assert.isNotNull(rewrite);
        Assert.isNotNull(workingUnit);

        ClassInstanceCreation primitiveTypeCreation = findPrimitiveTypeCreation(getASTNode(workingUnit,
                bug.getPrimarySourceLineAnnotation()));
        if (primitiveTypeCreation == null) {
            throw new BugResolutionException("Primitive type creation not found.");
        }
        MethodInvocation valueOfInvocation = createValueOfInvocation(rewrite, workingUnit, primitiveTypeCreation);
        rewrite.replace(primitiveTypeCreation, valueOfInvocation, null);
    }

    @CheckForNull
    protected ClassInstanceCreation findPrimitiveTypeCreation(ASTNode node) {
        PrimitiveTypeCreationFinder visitor = new PrimitiveTypeCreationFinder();
        node.accept(visitor);
        return visitor.getPrimitiveTypeCreation();
    }

    protected MethodInvocation createValueOfInvocation(ASTRewrite rewrite, CompilationUnit compilationUnit,
            ClassInstanceCreation primitiveTypeCreation) {
        Assert.isNotNull(rewrite);
        Assert.isNotNull(primitiveTypeCreation);

        final AST ast = rewrite.getAST();
        MethodInvocation valueOfInvocation = ast.newMethodInvocation();
        valueOfInvocation.setName(ast.newSimpleName(VALUE_OF_METHOD_NAME));

        ITypeBinding binding = primitiveTypeCreation.getType().resolveBinding();
        if (isStaticImport()) {
            addStaticImports(rewrite, compilationUnit, binding.getQualifiedName() + "." + VALUE_OF_METHOD_NAME);
        } else {
            valueOfInvocation.setExpression(ast.newSimpleName(binding.getName()));
        }

        List<?> arguments = primitiveTypeCreation.arguments();
        List<Expression> newArguments = valueOfInvocation.arguments();
        for (Object argument : arguments) {
            Expression expression = (Expression) rewrite.createCopyTarget((ASTNode) argument);
            newArguments.add(expression);
        }

        return valueOfInvocation;
    }

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected ASTVisitor getCustomLabelVisitor() {
        return new PrimitiveTypeCreationFinder();
    }

    @Override
    public void setOptions(@Nonnull Map<String, String> options) {
        // This setup (having two separate plugin.xml entries) is done to show off the
        // ApplicabilityVisitor, although it could be done without, just by having a
        // slightly fancier (and uglier) getLabelReplacement()
        isFloatingPoint = Boolean.parseBoolean(options.get("isFloatingPoint"));
        isDouble = Boolean.parseBoolean(options.get("isDouble"));
    }

    @Override
    protected ASTVisitor getApplicabilityVisitor() {
        return new PrimitiveTypeCreationFinder();
    }

    protected class PrimitiveTypeCreationFinder extends ASTVisitor implements CustomLabelVisitor, ApplicabilityVisitor {

        private ClassInstanceCreation primitiveTypeCreation = null;

        @Override
        public boolean visit(ClassInstanceCreation node) {
            if (primitiveTypeCreation == null) {
                if (!isPrimitiveTypeCreation(node)) {
                    return true;
                }
                this.primitiveTypeCreation = node;
            }
            return false;
        }

        public ClassInstanceCreation getPrimitiveTypeCreation() {
            return primitiveTypeCreation;
        }

        private boolean isPrimitiveTypeCreation(ClassInstanceCreation node) {
            ITypeBinding typeBinding = node.resolveTypeBinding();
            return primitiveWrapperClasses.contains(typeBinding.getQualifiedName());
        }

        @Override
        public String getLabelReplacement() {
            // returns what is in the constructor arguments
            if (primitiveTypeCreation == null || primitiveTypeCreation.arguments().isEmpty()) {
                return "...";   //safe return value
            }
            // can safely use toString() here because it's user facing, not being used as actual code
            return primitiveTypeCreation.arguments().get(0).toString();
        }

        @Override
        public boolean isApplicable() {
            // we must have found a primativeTypeCreation.  If we are not dealing with
            // DM_FP_NUMBER_CTOR, it's automatically applicable.  Otherwise,
            // we must match the isDouble argument with actually resolving a double
            return primitiveTypeCreation != null
                    && (!isFloatingPoint || (isDouble == "java.lang.Double".equals(primitiveTypeCreation.resolveTypeBinding()
                            .getQualifiedName())));
        }

    }

}
