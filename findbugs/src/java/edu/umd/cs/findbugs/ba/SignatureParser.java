/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;

/**
 * A simple class to parse method signatures.
 *
 * @author David Hovemeyer
 */
public class SignatureParser {
    private int totalArgumentSize;

    public int getTotalArgumentSize() {
        if ( parameterOffset == null) {
            getParameterOffset();
        }
        return totalArgumentSize;
    }

    private @CheckForNull int parameterOffset[];

    @Nonnull int[] getParameterOffset() {
        if ( parameterOffset != null ) {
            return parameterOffset;
        }
        ArrayList<Integer> offsets = new ArrayList<Integer>();
        Iterator<String> i = parameterSignatureIterator();
        int totalSize = 0;

        while (i.hasNext()) {
            String s = i.next();

            if ("D".equals(s) || "J".equals(s)) {
                totalSize += 2;
            } else {
                totalSize += 1;
            }
            offsets.add(totalSize);

        }
        totalArgumentSize = totalSize;
        int numParameters = offsets.size();
        parameterOffset = new int[numParameters];
        for (int j = 0; j < numParameters; j++) {
            parameterOffset[j] = offsets.get(j);
        }
        return parameterOffset;
    }

    public int getSlotsFromTopOfStackForParameter(int paramNum) {
        int offset = getParameterOffset()[paramNum];
        int result = totalArgumentSize - offset;
        return result;
    }

    private class ParameterSignatureIterator implements Iterator<String> {
        private int index = 1;

        @Override
        public boolean hasNext() {
            return index < signature.length() && signature.charAt(index) != ')';
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            StringBuilder result = new StringBuilder();
            boolean done;
            do {
                done = true;
                int ch = signature.charAt(index);
                switch (ch) {
                case 'B':
                case 'C':
                case 'D':
                case 'F':
                case 'I':
                case 'J':
                case 'S':
                case 'Z':
                    result.append(signature.charAt(index));
                    ++index;
                    break;

                case 'L':
                    int semi = signature.indexOf(';', index + 1);
                    if (semi < 0) {
                        throw new IllegalStateException("Invalid method signature: " + signature);
                    }
                    result.append(signature.substring(index, semi + 1));
                    index = semi + 1;
                    break;

                case '[':
                    result.append('[');
                    ++index;
                    done = false;
                    break;

                case 'V':
                default:
                    throw new IllegalStateException("Invalid method signature: " + signature);
                }
            } while (!done);

            return result.toString();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final String signature;

    @Override
    public String toString() {
        return signature;
    }

    /**
     * Constructor.
     *
     * @param signature
     *            the method signature to be parsed
     */
    public SignatureParser(String signature) {
        if (!signature.startsWith("(")) {
            throw new IllegalArgumentException("Bad method signature: " + signature);
        }
        this.signature = signature;


    }

    public String[] getArguments() {
        ArrayList<String> result = new ArrayList<String>();
        for (Iterator<String> i = parameterSignatureIterator(); i.hasNext();) {
            result.add(i.next());
        }
        return result.toArray(new String[result.size()]);
    }
    /**
     * Get an Iterator over signatures of the method parameters.
     *
     * @return Iterator which returns the parameter type signatures in order
     */
    public Iterator<String> parameterSignatureIterator() {
        return new ParameterSignatureIterator();
    }

    public Iterable<String> parameterSignatures() {
        return new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new ParameterSignatureIterator();
            }};

    }

    /**
     * Get the method return type signature.
     *
     * @return the method return type signature
     */
    public String getReturnTypeSignature() {
        int endOfParams = signature.lastIndexOf(')');
        if (endOfParams < 0) {
            throw new IllegalArgumentException("Bad method signature: " + signature);
        }
        return signature.substring(endOfParams + 1);
    }

    /**
     * Get the number of parameters in the signature.
     *
     * @return the number of parameters
     */
    public int getNumParameters() {
        return getParameterOffset().length;
    }

    public boolean hasReferenceParameters() {
        for (Iterator<String> i = parameterSignatureIterator(); i.hasNext();) {
            char c = i.next().charAt(0);
            if (c == 'L' || c == '[') {
                return true;
            }
        }
        return false;
    }

    public String getParameter(int pos) {
        int count = 0;
        for (Iterator<String> i = parameterSignatureIterator(); i.hasNext();) {
            String p = i.next();
            if (pos == count) {
                return p;
            }
            ++count;
        }
        throw new IndexOutOfBoundsException("Asked for parameter " + pos + " of " + signature);
    }

    /**
     * Determine whether or not given signature denotes a reference type.
     *
     * @param signature
     *            a signature
     * @return true if signature denotes a reference type, false otherwise
     */
    public static boolean isReferenceType(String signature) {
        return signature.startsWith("L") || signature.startsWith("[");
    }

    /**
     * Get the number of parameters passed to method invocation.
     *
     * @param inv
     * @param cpg
     * @return int number of parameters
     */
    public static int getNumParametersForInvocation(InvokeInstruction inv, ConstantPoolGen cpg) {
        SignatureParser sigParser = new SignatureParser(inv.getSignature(cpg));
        return sigParser.getNumParameters();
    }

    /**
     * Return how many stack frame slots a type whose signature is given will
     * occupy. long and double values take 2 slots, while all other kinds of
     * values take 1 slot.
     *
     * @param sig
     *            a type signature
     * @return number of stack frame slots a value of the given type will occupy
     */
    public static int getNumSlotsForType(String sig) {
        if ("J".equals(sig) || "D".equals(sig)) {
            return 2;
        } else {
            return 1;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: " + SignatureParser.class.getName() + " '<method signature>'");
            System.exit(1);
        }
        SignatureParser parser = new SignatureParser(args[0]);
        for (Iterator<String> i = parser.parameterSignatureIterator(); i.hasNext();) {
            System.out.println(i.next());
        }
        System.out.println(parser.getNumParameters() + " parameter(s)");
    }
}

