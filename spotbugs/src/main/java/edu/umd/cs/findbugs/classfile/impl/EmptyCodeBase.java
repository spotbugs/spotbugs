package edu.umd.cs.findbugs.classfile.impl;

import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import java.util.NoSuchElementException;

public class EmptyCodeBase extends AbstractScannableCodeBase {

    public EmptyCodeBase(ICodeBaseLocator codeBaseLocator) {
        super(codeBaseLocator);
    }

    @Override
    public ICodeBaseIterator iterator() throws InterruptedException {
        return new ICodeBaseIterator() {
            @Override
            public boolean hasNext() throws InterruptedException {
                return false;
            }

            @Override
            public ICodeBaseEntry next() throws InterruptedException {
                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public ICodeBaseEntry lookupResource(String resourceName) {
        return null;
    }

    @Override
    public String getPathName() {
        return null;
    }

    @Override
    public void close() {

    }
}
