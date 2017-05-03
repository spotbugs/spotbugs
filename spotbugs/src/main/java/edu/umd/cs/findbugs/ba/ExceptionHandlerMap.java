/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckForNull;

import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.type.TypeMerger;

/**
 * This class provides a convenient way of determining the exception handlers
 * for instructions in a method. Essentially, it's a a map of InstructionHandles
 * to lists of CodeExceptionGen objects. This class also maps instructions which
 * are the start of exception handlers to the CodeExceptionGen object
 * representing the handler.
 *
 * @author David Hovemeyer
 */
public class ExceptionHandlerMap {
    private final IdentityHashMap<InstructionHandle, List<CodeExceptionGen>> codeToHandlerMap;

    private final IdentityHashMap<InstructionHandle, CodeExceptionGen> startInstructionToHandlerMap;

    private final TypeMerger merger;
    /**
     * Constructor.
     *
     * @param methodGen
     *            the method to build the map for
     */
    public ExceptionHandlerMap( MethodGen methodGen, TypeMerger merger) {
        codeToHandlerMap = new IdentityHashMap<InstructionHandle, List<CodeExceptionGen>>();
        startInstructionToHandlerMap = new IdentityHashMap<InstructionHandle, CodeExceptionGen>();
        this.merger = merger;
        build(methodGen);
    }

    /**
     * Get the list of exception handlers (CodeExceptionGen objects) which are
     * specified to handle exceptions for the instruction whose handle is given.
     * Note that the handlers in the returned list are <b>in order of
     * priority</b>, as defined in the method's exception handler table.
     *
     * @param handle
     *            the handle of the instruction we want the exception handlers
     *            for
     * @return the list of exception handlers, or null if there are no handlers
     *         registered for the instruction
     */
    public List<CodeExceptionGen> getHandlerList(InstructionHandle handle) {
        return codeToHandlerMap.get(handle);
    }

    /**
     * If the given instruction is the start of an exception handler, get the
     * CodeExceptionGen object representing the handler.
     *
     * @param start
     *            the instruction
     * @return the CodeExceptionGen object, or null if the instruction is not
     *         the start of an exception handler
     */
    public CodeExceptionGen getHandlerForStartInstruction(InstructionHandle start) {
        return startInstructionToHandlerMap.get(start);
    }

    private void build(MethodGen methodGen) {
        CodeExceptionGen[] handlerList = methodGen.getExceptionHandlers();

        // Map handler start instructions to the actual exception handlers
        for (CodeExceptionGen exceptionHandler : handlerList) {
            addExceptionHandler(exceptionHandler);
        }

        // For each instruction, determine which handlers it can reach
        InstructionHandle handle = methodGen.getInstructionList().getStart();
        while (handle != null) {
            int offset = handle.getPosition();

            handlerLoop: for (CodeExceptionGen exceptionHandler : handlerList) {
                int startOfRange = exceptionHandler.getStartPC().getPosition();
                int endOfRange = exceptionHandler.getEndPC().getPosition();

                if (offset >= startOfRange && offset <= endOfRange) {
                    // This handler is reachable from the instruction
                    addHandler(handle, exceptionHandler);

                    // If this handler handles all exception types
                    // i.e., an ANY handler, or catch(Throwable...),
                    // then no further (lower-priority)
                    // handlers are reachable from the instruction.
                    if (Hierarchy.isUniversalExceptionHandler(exceptionHandler.getCatchType())) {
                        break handlerLoop;
                    }
                }
            }

            handle = handle.getNext();
        }
    }

    public static CodeExceptionGen merge(@CheckForNull TypeMerger m, CodeExceptionGen e1, CodeExceptionGen e2) {
        if (e1 == null) {
            return e2;
        }
        if (e2 == null) {
            return e1;
        }
        if (m == null) {
            return e1;
        }
        if ( ! e1.getHandlerPC().equals( e2.getHandlerPC() ) ){
            // log error
            return e1;
        }
        try {
            Type t = m.mergeTypes(e1.getCatchType(), e2.getCatchType());
            return new CodeExceptionGen(e1.getStartPC(), e1.getEndPC(), e1.getHandlerPC(), (ObjectType) t);
        } catch (DataflowAnalysisException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return e1;
        }
    }


    private void addExceptionHandler(CodeExceptionGen exceptionHandler) {
        InstructionHandle handlerPC = exceptionHandler.getHandlerPC();
        CodeExceptionGen existing = startInstructionToHandlerMap.get(handlerPC);
        if (existing != null) {
            exceptionHandler = merge (this.merger, existing, exceptionHandler);
        }
        startInstructionToHandlerMap.put(handlerPC, exceptionHandler);
    }

    private void addHandler(InstructionHandle handle, CodeExceptionGen exceptionHandler) {
        List<CodeExceptionGen> handlerList = codeToHandlerMap.get(handle);
        if (handlerList == null) {
            handlerList = new LinkedList<CodeExceptionGen>();
            codeToHandlerMap.put(handle, handlerList);
        }
        handlerList.add(exceptionHandler);
    }
}

