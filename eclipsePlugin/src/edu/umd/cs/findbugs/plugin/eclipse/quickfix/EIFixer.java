package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import java.util.Iterator;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.umd.cs.findbugs.BugInstance;

public class EIFixer extends FindBugsFixer {
	public boolean resolveBindings() {return true;}

	@Override
	protected boolean modify(ASTRewrite rewrite, CompilationUnit astRoot, BugInstance bug) {
		TypeDeclaration type;
		MethodDeclaration method;
		
		String className = bug.getPrimaryClass().getClassName();
		String methodNameSig = bug.getPrimaryMethod().getNameInClass(false);
		String fieldName = bug.getPrimaryField().getFieldName();
		
		Expression retEx = null;
		CastExpression castRet = null;
		Expression original = null;
		Expression cloneField;
		MethodInvocation cloneInvoke;
		SimpleName cloneName;
		
		type = getTypeDeclaration(astRoot, className);
		if (type == null)
			return false;
		
		method = getMethodDeclaration(type, methodNameSig);
		if (method == null)
			return false;
		
		Iterator itr = method.getBody().statements().iterator();
		
		while (itr.hasNext() && original == null) {
			Statement stmt = (Statement)itr.next();
			if (!(stmt instanceof ReturnStatement))
				continue;
			retEx = ((ReturnStatement)stmt).getExpression();

			if (retEx instanceof SimpleName && ((SimpleName)retEx).getIdentifier().equals(fieldName)) {
				original = (SimpleName)retEx;
			}
			else if (retEx instanceof FieldAccess && isThisFieldAccess((FieldAccess)retEx, fieldName)) {
				original = ((FieldAccess)retEx).getName();
			}
		}
		
		if (original == null)
			return false;
		
		//set up the clone part
		cloneInvoke = astRoot.getAST().newMethodInvocation();
		cloneField = (SimpleName) ASTNode.copySubtree(cloneInvoke.getAST(), original);
		cloneName = astRoot.getAST().newSimpleName("clone");
		
		cloneInvoke.setExpression(cloneField);
		cloneInvoke.setName(cloneName);
		
		
		//cast the result to the right type
		Type retType;
		castRet = astRoot.getAST().newCastExpression();
		retType = (Type)ASTNode.copySubtree(castRet.getAST(), method.getReturnType2());
		castRet.setExpression(cloneInvoke);
		castRet.setType(retType);
		

		rewrite.replace(original, castRet, null);

		return true;
	}

	private boolean isThisFieldAccess(FieldAccess access, String fieldName) {
		return (access.getExpression() instanceof ThisExpression) &&
		 access.getName().getIdentifier().equals(fieldName);
	}

	public String getLabel() {
		return "Clone the field (Think about performance first!)";
	}

}
