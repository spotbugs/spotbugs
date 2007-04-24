package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getMethodDeclaration;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getTypeDeclaration;

import java.util.Iterator;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * Returning a reference to a mutable object is not recommended. The class 
 * <CODE>CreateMutableCloneResolution</CODE> returns a new copy of the object.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#EI_EXPOSE_REP">EI_EXPOSE_REP</a>
 */
public class CreateMutableCloneResolution extends BugResolution {

	@Override
	public boolean resolveBindings() {
		return true;
    }

	@Override
	protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
		assert rewrite != null;
        assert workingUnit != null;
		assert bug != null;

		TypeDeclaration type = getTypeDeclaration(workingUnit, bug.getPrimaryClass());
		MethodDeclaration method = getMethodDeclaration(type, bug.getPrimaryMethod());

		String fieldName = bug.getPrimaryField().getFieldName();

		Expression retEx = null;
		CastExpression castRet = null;
		Expression original = null;
        Expression cloneField;
		MethodInvocation cloneInvoke;
		SimpleName cloneName;

		Iterator<?> itr = method.getBody().statements().iterator();

		while (itr.hasNext() && original == null) {
			Statement stmt = (Statement) itr.next();
			if (!(stmt instanceof ReturnStatement)) {
                continue;
			}
			retEx = ((ReturnStatement) stmt).getExpression();

			if (retEx instanceof SimpleName && ((SimpleName) retEx).getIdentifier().equals(fieldName)) {
				original = retEx;
			} else if (retEx instanceof FieldAccess && isThisFieldAccess((FieldAccess) retEx, fieldName)) {
                original = ((FieldAccess) retEx).getName();
			}
		}

		if (original == null) {
			throw new BugResolutionException("No original field found.");
		}

		// set up the clone part
		cloneInvoke = workingUnit.getAST().newMethodInvocation();
		cloneField = (SimpleName) ASTNode.copySubtree(cloneInvoke.getAST(), original);
        cloneName = workingUnit.getAST().newSimpleName("clone");

		cloneInvoke.setExpression(cloneField);
		cloneInvoke.setName(cloneName);

		// cast the result to the right type
		Type retType;
		castRet = workingUnit.getAST().newCastExpression();
        retType = (Type) ASTNode.copySubtree(castRet.getAST(), method.getReturnType2());
		castRet.setExpression(cloneInvoke);
		castRet.setType(retType);

		rewrite.replace(original, castRet, null);

	}

	private boolean isThisFieldAccess(FieldAccess access, String fieldName) {
		return (access.getExpression() instanceof ThisExpression) && access.getName().getIdentifier().equals(fieldName);
	}

}
