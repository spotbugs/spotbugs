/*
 * Contributions to FindBugs
 * Copyright (C) 2003, Institut for Software
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

import static org.eclipse.jdt.core.dom.InfixExpression.Operator.NOT_EQUALS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.REMAINDER;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/**
 * The <CODE>CreateAndOddnessCheckResolution</CODE> is a subclass of the
 * abstract class <CODE>CorrectOddnessCheckResolution</CODE> and creates the
 * proper oddness check <CODE>(x % 2) != 0</CODE>.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#IM_BAD_CHECK_FOR_ODD">IM_BAD_CHECK_FOR_ODD</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @version 1.0
 */
public class CreateRemainderOddnessCheckResolution extends CorrectOddnessCheckResolution {

    /**
     * Creates the new <CODE>InfixExpression</CODE> <CODE>x % 2 != 0</CODE>
     */
    @Override
    protected InfixExpression createCorrectOddnessCheck(ASTRewrite rewrite, Expression numberExpression) {
        assert rewrite != null;
        assert numberExpression != null;

        final AST ast = rewrite.getAST();
        InfixExpression correctOddnessCheck = ast.newInfixExpression();
        InfixExpression remainderExp = ast.newInfixExpression();

        correctOddnessCheck.setLeftOperand(remainderExp);
        correctOddnessCheck.setOperator(NOT_EQUALS);
        correctOddnessCheck.setRightOperand(ast.newNumberLiteral("0"));

        remainderExp.setLeftOperand((Expression) rewrite.createMoveTarget(numberExpression));
        remainderExp.setOperator(REMAINDER);
        remainderExp.setRightOperand(ast.newNumberLiteral("2"));

        return correctOddnessCheck;
    }

}
