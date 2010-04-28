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
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getTypeDeclaration;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * Some methods should implement a specific <CODE>super</CODE>-call for a
 * clean execution of the code. The <CODE>CreateSuperCallResolution</CODE>
 * creates a new <CODE>super</CODE>-call for these methods.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#FI_MISSING_SUPER_CALL">FI_MISSING_SUPER_CALL</a>
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#IJU_SETUP_NO_SUPER">IJU_SETUP_NO_SUPER</a>
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#IJU_TEARDOWN_NO_SUPER">IJU_TEARDOWN_NO_SUPER</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:g1zgragg@hsr.ch">Guido Zgraggen</a>
 * @version 1.0
 */
public class CreateSuperCallResolution extends BugResolution {

	private boolean insertFirst = true;

	public CreateSuperCallResolution() {
		super();
	}

	public CreateSuperCallResolution(boolean insertFirst) {
		this();
		setInsertFirst(insertFirst);
    }

	public boolean isInsertFirst() {
		return insertFirst;
	}

	public void setInsertFirst(boolean insertFirst) {
		this.insertFirst = insertFirst;
	}

	public boolean isInsertLast() {
		return !isInsertFirst();
	}

	public void setInsertLast(boolean insertLast) {
		setInsertFirst(!insertLast);
	}

	@Override
	protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
		assert rewrite != null;
        assert workingUnit != null;
		assert bug != null;

		TypeDeclaration type = getTypeDeclaration(workingUnit, bug.getPrimaryClass());
		MethodDeclaration method = getMethodDeclaration(type, bug.getPrimaryMethod());

		AST ast = rewrite.getAST();

		SuperMethodInvocation superCall = createSuperMethodInvocation(rewrite, method);
		ExpressionStatement statement = ast.newExpressionStatement(superCall);
		Block methodBody = method.getBody();
        ListRewrite listRewrite = rewrite.getListRewrite(methodBody, Block.STATEMENTS_PROPERTY);
		if (isInsertFirst()) {
			listRewrite.insertFirst(statement, null);
		} else {
            listRewrite.insertLast(statement, null);
		}
	}

	protected SuperMethodInvocation createSuperMethodInvocation(ASTRewrite rewrite, MethodDeclaration method) {
		assert rewrite != null;
		assert method != null;

		AST ast = rewrite.getAST();
		SuperMethodInvocation invocation = ast.newSuperMethodInvocation();

		invocation.setName((SimpleName) rewrite.createCopyTarget(method.getName()));

		return invocation;
	}

	@Override
	protected boolean resolveBindings() {
		return true;
    }

}
