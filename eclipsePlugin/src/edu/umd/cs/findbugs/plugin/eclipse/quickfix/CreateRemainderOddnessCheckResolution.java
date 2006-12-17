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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

/**
 * The <CODE>CreateAndOddnessCheckResolution</CODE> is a subclass of the abstract
 * class <CODE>CorrectOddnessCheckResolution</CODE> and creates the proper oddness
 * check <CODE>(x % 2) != 0</CODE>.
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
    protected InfixExpression createReplaceExpression(AST ast, SimpleName replaceField) {
        assert ast != null;
        assert replaceField != null;
        InfixExpression replaceExpression = ast.newInfixExpression();
        InfixExpression replaceExLeftOperand = ast.newInfixExpression();
        replaceExLeftOperand.setOperator(Operator.REMAINDER);
        replaceExLeftOperand.setLeftOperand(replaceField);
        NumberLiteral replaceExLeftOperandROP = ast.newNumberLiteral("2");
        replaceExLeftOperand.setRightOperand(replaceExLeftOperandROP);
        replaceExpression.setLeftOperand(replaceExLeftOperand);
        replaceExpression.setOperator(Operator.NOT_EQUALS);
        NumberLiteral replaceRightOperand = ast.newNumberLiteral("0");
        replaceExpression.setRightOperand(replaceRightOperand);
        return replaceExpression;
    }
}
