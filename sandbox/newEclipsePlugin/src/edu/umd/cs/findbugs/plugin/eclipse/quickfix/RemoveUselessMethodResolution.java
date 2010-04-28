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

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * Empty <CODE>finalize()</CODE> methods are useless, so they should be
 * deleted. The same is valid for <CODE>finalize()</CODE> methods that only
 * call the superclass's <CODE>finalize()</CODE> method. 
 * The class <CODE>RemoveUselessMethodResolution</CODE> removes such useless methods.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#FI_EMPTY">FI_EMPTY</a>
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#FI_USELESS">FI_USELESS</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @version 1.0
 */
public class RemoveUselessMethodResolution extends BugResolution {

	@Override
	protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
		assert rewrite != null;
        assert workingUnit != null;
		assert bug != null;

		TypeDeclaration type = getTypeDeclaration(workingUnit, bug.getPrimaryClass());
		MethodDeclaration method = getMethodDeclaration(type, bug.getPrimaryMethod());
		rewrite.remove(method, null);
    }

	@Override
	protected boolean resolveBindings() {
		return true;
    }

}
