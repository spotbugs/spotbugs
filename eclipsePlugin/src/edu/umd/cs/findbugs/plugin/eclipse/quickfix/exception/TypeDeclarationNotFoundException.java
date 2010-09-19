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
package edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception;

import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Thrown when no <CODE>FieldDeclaration</CODE> was found in the specified
 * <CODE>TypeDeclaration</CODE>.
 * 
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @version 1.0
 */
public class TypeDeclarationNotFoundException extends BodyDeclarationNotFoundException {

    private static final long serialVersionUID = 1350287531280130380L;

    private final CompilationUnit compilationUnit;

    private final String className;

    public TypeDeclarationNotFoundException(CompilationUnit compilationUnit, String className, String message) {
        super("Type declaration '" + className + "' not found in the specified compilation unit: " + message);
        this.compilationUnit = compilationUnit;
        this.className = className;
    }

    public TypeDeclarationNotFoundException(CompilationUnit compilationUnit, String className) {
        this(compilationUnit, className, "");
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    public String getClassName() {
        return className;
    }

}
