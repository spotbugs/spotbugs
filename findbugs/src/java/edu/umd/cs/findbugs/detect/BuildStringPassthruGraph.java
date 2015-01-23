/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.detect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Builds the database of string parameters passed from method to method unchanged.
 * @author Tagir Valeev
 */
public class BuildStringPassthruGraph extends OpcodeStackDetector implements NonReportingDetector {

    public static class MethodParameter {
        final MethodDescriptor md;

        final int parameterNumber;

        public MethodParameter(MethodDescriptor md, int parameterNumber) {
            super();
            this.md = md;
            this.parameterNumber = parameterNumber;
        }

        public MethodDescriptor getMethodDescriptor() {
            return md;
        }

        public int getParameterNumber() {
            return parameterNumber;
        }

        @Override
        public String toString() {
            return this.md + "[" + this.parameterNumber + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((md == null) ? 0 : md.hashCode());
            result = prime * result + parameterNumber;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MethodParameter other = (MethodParameter) obj;
            if (md == null) {
                if (other.md != null) {
                    return false;
                }
            } else if (!md.equals(other.md)) {
                return false;
            }
            if (parameterNumber != other.parameterNumber) {
                return false;
            }
            return true;
        }
    }

    public static class StringPassthruDatabase {
        private static final List<MethodDescriptor> FILENAME_STRING_METHODS = Arrays.asList(
                new MethodDescriptor("java/io/File", "<init>", "(Ljava/lang/String;)V"),
                new MethodDescriptor("java/io/File", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"),
                new MethodDescriptor("java/io/RandomAccessFile", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"),
                new MethodDescriptor("java/nio/file/Paths", "get", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;", true),
                new MethodDescriptor("java/io/FileReader", "<init>", "(Ljava/lang/String;)V"),
                new MethodDescriptor("java/io/FileWriter", "<init>", "(Ljava/lang/String;)V"),
                new MethodDescriptor("java/io/FileWriter", "<init>", "(Ljava/lang/String;Z)V"),
                new MethodDescriptor("java/io/FileInputStream", "<init>", "(Ljava/lang/String;)V"),
                new MethodDescriptor("java/io/FileOutputStream", "<init>", "(Ljava/lang/String;)V"),
                new MethodDescriptor("java/io/FileOutputStream", "<init>", "(Ljava/lang/String;Z)V"),
                new MethodDescriptor("java/util/Formatter", "<init>", "(Ljava/lang/String;)V"),
                new MethodDescriptor("java/util/Formatter", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"),
                new MethodDescriptor("java/util/Formatter", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/util/Locale;)V"),
                new MethodDescriptor("java/util/jar/JarFile", "<init>", "(Ljava/lang/String;)V"),
                new MethodDescriptor("java/util/jar/JarFile", "<init>", "(Ljava/lang/String;Z)V"),
                new MethodDescriptor("java/util/zip/ZipFile", "<init>", "(Ljava/lang/String;)V"),
                new MethodDescriptor("java/util/zip/ZipFile", "<init>", "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"),
                new MethodDescriptor("java/io/PrintStream", "<init>", "(Ljava/lang/String;)V"),
                new MethodDescriptor("java/io/PrintStream", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"),
                new MethodDescriptor("java/io/PrintWriter", "<init>", "(Ljava/lang/String;)V"),
                new MethodDescriptor("java/io/PrintWriter", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V")
                );

        private final Map<MethodParameter, Set<MethodParameter>> graph = new HashMap<>();

        /**
         * Adds edge to the string passthru graph
         * @param in callee
         * @param out caller
         */
        void addEdge(MethodParameter in, MethodParameter out) {
            Set<MethodParameter> outs = graph.get(in);
            if (outs == null) {
                outs = new HashSet<>();
                graph.put(in, outs);
            }
            outs.add(out);
        }

        Set<MethodParameter> findLinked(Set<MethodParameter> inputs) {
            Set<MethodParameter> result = new HashSet<>(inputs);
            Queue<MethodParameter> toCheck = new ArrayDeque<>(inputs);
            while (!toCheck.isEmpty()) {
                MethodParameter in = toCheck.poll();
                Set<MethodParameter> outs = graph.get(in);
                if (outs != null) {
                    for (MethodParameter out : outs) {
                        if (!result.contains(out)) {
                            result.add(out);
                            toCheck.add(out);
                        }
                    }
                }
            }
            return result;
        }

        /**
         * Returns methods which call directly or indirectly methods from inputs
         * passing the parameter unchanged
         *
         * @param inputs
         *            input methods with parameter
         * @return Map where keys are methods and values are parameter indexes which can be passed to requested methods unchanged
         */
        public Map<MethodDescriptor, int[]> findLinkedMethods(Set<MethodParameter> inputs) {
            Map<MethodDescriptor, int[]> result = new HashMap<>();
            for (MethodParameter found : findLinked(inputs)) {
                int[] params = result.get(found.getMethodDescriptor());
                if(params == null) {
                    params = new int[] {found.getParameterNumber()};
                    result.put(found.getMethodDescriptor(), params);
                } else {
                    int[] newParams = new int[params.length+1];
                    System.arraycopy(params, 0, newParams, 0, params.length);
                    newParams[params.length] = found.getParameterNumber();
                    result.put(found.getMethodDescriptor(), newParams);
                }
            }
            return result;
        }

        /**
         * Returns methods which parameter is the file name
         * @return Map where keys are methods and values are parameter indexes which are used as file names
         */
        public Map<MethodDescriptor, int[]> getFileNameStringMethods() {
            Set<MethodParameter> fileNameStringMethods = new HashSet<>();
            for(MethodDescriptor md : FILENAME_STRING_METHODS) {
                fileNameStringMethods.add(new MethodParameter(md, 0));
            }
            return findLinkedMethods(fileNameStringMethods);
        }
    }

    private final StringPassthruDatabase cache = new StringPassthruDatabase();

    private int nArgs;

    private int shift;

    private boolean[] argEnabled;

    private List<MethodParameter>[] passedParameters;

    public BuildStringPassthruGraph(BugReporter bugReporter) {
        Global.getAnalysisCache().eagerlyPutDatabase(StringPassthruDatabase.class, cache);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visitMethod(Method obj) {
        argEnabled = null;
        org.apache.bcel.generic.Type[] argumentTypes = obj.getArgumentTypes();
        if(argumentTypes.length == 0) {
            return;
        }
        nArgs = argumentTypes.length;
        for(int i=0; i<nArgs; i++) {
            if(argumentTypes[i].getSignature().equals("Ljava/lang/String;")) {
                if(argEnabled == null) {
                    argEnabled = new boolean[nArgs];
                }
                argEnabled[i] = true;
            }
        }
        if(argEnabled != null) {
            shift = obj.isStatic() ? 0 : -1;
            passedParameters = new List[nArgs];
        }
        super.visitMethod(obj);
    }

    @Override
    public boolean shouldVisitCode(Code obj) {
        return argEnabled != null;
    }

    @Override
    public void visitAfter(Code obj) {
        super.visitAfter(obj);
        for (int i = 0; i < nArgs; i++) {
            List<MethodParameter> list = passedParameters[i];
            if (list != null) {
                MethodParameter cur = new MethodParameter(getMethodDescriptor(), i);
                for (MethodParameter mp : list) {
                    cache.addEdge(mp, cur);
                }
            }
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (isRegisterStore()) {
            int param = getRegisterOperand() + shift;
            if (param >= 0 && param < nArgs) {
                argEnabled[param] = false;
                passedParameters[param] = null;
            }
        }
        switch (seen) {
        case INVOKESPECIAL:
        case INVOKESTATIC:
        case INVOKEINTERFACE:
        case INVOKEVIRTUAL:
            MethodDescriptor md = getMethodDescriptorOperand();
            int callArgs = getNumberArguments(md.getSignature());
            for (int i = 0; i < callArgs; i++) {
                Item item = getStack().getStackItem(callArgs - 1 - i);
                int param = item.getRegisterNumber() + shift;
                if (param >= 0 && param < nArgs && argEnabled[param]) {
                    List<MethodParameter> list = passedParameters[param];
                    if (list == null) {
                        passedParameters[param] = list = new ArrayList<>();
                    }
                    list.add(new MethodParameter(md, i));
                }
            }
            break;
        default:
            break;
        }
    }
}
