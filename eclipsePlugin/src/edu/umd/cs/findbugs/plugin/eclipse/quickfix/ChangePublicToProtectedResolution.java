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
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getTypeDeclaration;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PROTECTED_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;

import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * Methods that should be accessed only by the class itself or by direct
 * subclasses should have <CODE>protected</CODE> access, not <CODE>public</CODE>.
 * The class <CODE>ChangePublicToProtectedResolution</CODE> replaces the
 * modifier of such methods by the modifier <CODE>protected</CODE>.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#FI_PUBLIC_SHOULD_BE_PROTECTED">FI_PUBLIC_SHOULD_BE_PROTECTED</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @version 1.0
 */
public class ChangePublicToProtectedResolution extends BugResolution {

	@Override
	protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
		assert rewrite != null;
        assert workingUnit != null;
		assert bug != null;

		TypeDeclaration type = getTypeDeclaration(workingUnit, bug.getPrimaryClass());
		MethodDeclaration method = getMethodDeclaration(type, bug.getPrimaryMethod());
		Modifier originalModifier = getPublicModifier(method);

		ListRewrite listRewrite = rewrite.getListRewrite(method, MethodDeclaration.MODIFIERS2_PROPERTY);
		Modifier protectedModifier = workingUnit.getAST().newModifier(PROTECTED_KEYWORD);
		listRewrite.replace(originalModifier, protectedModifier, null);
    }

	private Modifier getPublicModifier(MethodDeclaration method) {
		List<?> list = method.modifiers();
		for (Object o : list) {
            if (o.getClass().equals(Modifier.class)) {
				Modifier mdf = (Modifier) o;
				if (mdf.getKeyword().equals(PUBLIC_KEYWORD)) {
					return mdf;
                }
			}

		}
		return null;
	}

	@Override
	protected boolean resolveBindings() {
		return true;
    }

}
