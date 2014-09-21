package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getMethodDeclaration;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getTypeDeclaration;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
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
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * Returning a reference to a mutable object is not recommended. The class
 * <CODE>CreateMutableCloneResolution</CODE> returns a new copy of the object.
 *
 * @see <a
 *      href="http://findbugs.sourceforge.net/bugDescriptions.html#EI_EXPOSE_REP">EI_EXPOSE_REP</a>
 */
public class CreateMutableCloneResolution extends BugResolution {

    @Override
    public boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        Assert.isNotNull(rewrite);
        Assert.isNotNull(workingUnit);
        Assert.isNotNull(bug);

        TypeDeclaration type = getTypeDeclaration(workingUnit, bug.getPrimaryClass());
        MethodDeclaration method = getMethodDeclaration(type, bug.getPrimaryMethod());


        FieldAnnotation primaryField = bug.getPrimaryField();
        if (primaryField == null) {
            throw new BugResolutionException("No original field found.");
        }
        String fieldName = primaryField.getFieldName();
        SimpleName original = null;

        for (Statement stmt : (List<Statement>) method.getBody().statements()) {
            if (stmt instanceof ReturnStatement) {
                Expression retEx = ((ReturnStatement) stmt).getExpression();

                if (retEx instanceof SimpleName && ((SimpleName) retEx).getIdentifier().equals(fieldName)) {
                    original = (SimpleName) retEx;
                    break;
                } else if (retEx instanceof FieldAccess && isThisFieldAccess((FieldAccess) retEx, fieldName)) {
                    original = ((FieldAccess) retEx).getName();
                    break;
                }
            }
        }

        if (original == null) {
            throw new BugResolutionException("No original field found.");
        }

        // set up the clone part
        MethodInvocation cloneInvoke = invokeClone(workingUnit, original);



        // cast the result to the right type
        CastExpression castRet = workingUnit.getAST().newCastExpression();
        castRet.setExpression(cloneInvoke);
        Type retType = (Type) ASTNode.copySubtree(castRet.getAST(), method.getReturnType2());
        castRet.setType(retType);


        ConditionalExpression conditionalExpression = workingUnit.getAST().newConditionalExpression();
        conditionalExpression.setElseExpression(castRet);
        conditionalExpression.setThenExpression(workingUnit.getAST().newNullLiteral() );

        InfixExpression nullTest = workingUnit.getAST().newInfixExpression();
        nullTest.setOperator(InfixExpression.Operator.EQUALS);
        nullTest.setRightOperand(workingUnit.getAST().newNullLiteral());
        Expression initialLoad = (SimpleName) ASTNode.copySubtree(cloneInvoke.getAST(), original);
        nullTest.setLeftOperand(initialLoad);

        conditionalExpression.setExpression(nullTest);

        rewrite.replace(original, conditionalExpression, null);

    }

    /**
     * @param workingUnit
     * @param original
     * @return
     */
    private MethodInvocation invokeClone(CompilationUnit workingUnit, SimpleName original) {
        MethodInvocation cloneInvoke = workingUnit.getAST().newMethodInvocation();
        Expression cloneField = (SimpleName) ASTNode.copySubtree(cloneInvoke.getAST(), original);
        SimpleName cloneName = workingUnit.getAST().newSimpleName("clone");
        cloneInvoke.setExpression(cloneField);
        cloneInvoke.setName(cloneName);
        return cloneInvoke;
    }

    private boolean isThisFieldAccess(FieldAccess access, String fieldName) {
        return (access.getExpression() instanceof ThisExpression) && access.getName().getIdentifier().equals(fieldName);
    }

}
