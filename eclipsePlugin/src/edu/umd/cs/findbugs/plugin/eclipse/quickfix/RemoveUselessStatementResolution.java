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

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * Statements that are useless should be removed from the code. The <CODE>RemoveUselessStatementResolution</CODE>
 * removes such statements.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#SA_FIELD_SELF_ASSIGNMENT">SA_FIELD_SELF_ASSIGNMENT</a>
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#SA_LOCAL_SELF_ASSIGNMENT">SA_LOCAL_SELF_ASSIGNMENT</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @version 1.0
 */

public class RemoveUselessStatementResolution extends BugResolution {

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        assert rewrite != null;
        assert workingUnit != null;
        assert bug != null;

        TypeDeclaration type = getTypeDeclaration(workingUnit, bug.getPrimaryClass());
        MethodDeclaration method = getMethodDeclaration(type, bug.getPrimaryMethod());
        Statement statement = getStatement(workingUnit, method, bug.getPrimarySourceLineAnnotation());

        rewrite.remove(statement, null);
    }

    @Override
    protected boolean resolveBindings() {
        return true;
    }

}
