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

import java.util.List;

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

	private static final String VALUE_OF_METHOD_NAME = "valueOf";

	private boolean staticImport = false;

	public UseValueOfResolution() {
		super();
	}

	public UseValueOfResolution(boolean staticImport) {
		this();
		this.staticImport = staticImport;
    }

	public boolean isStaticImport() {
		return staticImport;
	}

	public void setStaticImport(boolean staticImport) {
		this.staticImport = staticImport;
	}

	@Override
	protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
		assert rewrite != null;
        assert workingUnit != null;

		ClassInstanceCreation primitiveTypeCreation = findPrimitiveTypeCreation(getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation()));
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

	protected MethodInvocation createValueOfInvocation(ASTRewrite rewrite, CompilationUnit compilationUnit, ClassInstanceCreation primitiveTypeCreation) {
		assert rewrite != null;
		assert primitiveTypeCreation != null;

		final AST ast = rewrite.getAST();
		MethodInvocation valueOfInvocation = ast.newMethodInvocation();
		valueOfInvocation.setName(ast.newSimpleName(VALUE_OF_METHOD_NAME));

		ITypeBinding binding = primitiveTypeCreation.getType().resolveBinding();
		if (isStaticImport()) {
			addStaticImports(rewrite, compilationUnit, binding.getQualifiedName() + "." + VALUE_OF_METHOD_NAME);
        } else {
			valueOfInvocation.setExpression(ast.newSimpleName(binding.getName()));
		}

		List<Expression> arguments = primitiveTypeCreation.arguments();
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

	protected static class PrimitiveTypeCreationFinder extends ASTVisitor {

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

		private boolean isPrimitiveTypeCreation(ClassInstanceCreation primitiveTypeCreation) {
			// TODO check if the ClassInstanceCreation is a primitive type
			// creation
            return true;
		}

	}

}
