/*
 * Contributions to SpotBugs
 * Copyright (C) 2019, kengo
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
package edu.umd.cs.findbugs.bcel;

import static java.util.Objects.requireNonNull;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.Repository;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * <p>
 * A thin wrapper to synchronize access to mutable BCEL repository.
 * 
 * @since 4.0
 */
public class ConcurrentRepository implements Repository {
    private final Repository delegate;

    /**
     * @param delegate
     *            non-null {@link Repository} instance to wrap.
     */
    public ConcurrentRepository(@NonNull Repository delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public synchronized void clear() {
        delegate.clear();
    }

    @Override
    public synchronized JavaClass findClass(String className) {
        return delegate.findClass(className);
    }

    @Override
    public synchronized ClassPath getClassPath() {
        return delegate.getClassPath();
    }

    @Override
    public synchronized JavaClass loadClass(String className) throws ClassNotFoundException {
        return delegate.loadClass(className);
    }

    @Override
    public synchronized JavaClass loadClass(Class<?> clazz) throws ClassNotFoundException {
        return delegate.loadClass(clazz);
    }

    @Override
    public synchronized void removeClass(JavaClass clazz) {
        delegate.removeClass(clazz);
    }

    @Override
    public synchronized void storeClass(JavaClass clazz) {
        delegate.storeClass(clazz);
    }

}
