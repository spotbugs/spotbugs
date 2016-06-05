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

package edu.umd.cs.findbugs.classfile.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 *
 * Code base supporting Java 9 new jimage packed modules
 *
 * @author andrey
 */
public class JrtfsCodeBase extends AbstractScannableCodeBase {

    private FileSystem fs;
    private final String fileName;
    private Path root;
    private Map<String, String> packageToModuleMap;

    public JrtfsCodeBase(ICodeBaseLocator codeBaseLocator, @Nonnull String fileName) {
        super(codeBaseLocator);
        this.fileName = fileName;
        URL url;
        try {
            url = Paths.get(fileName).toUri().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[] { url });
            fs = FileSystems.newFileSystem(URI.create("jrt:/"), Collections.emptyMap(), loader);
            root = fs.getPath("modules");
            packageToModuleMap = createPackageToModuleMap(fs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> createPackageToModuleMap(FileSystem fs) throws IOException{
        HashMap<String, String> packageToModule = new LinkedHashMap<>();
        Path path = fs.getPath("packages");
        Files.list(path).forEach(p -> {
            try {
                Files.list(p).findFirst().ifPresent(c -> packageToModule.put(fileName(p).replace('.', '/') , fileName(c)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return packageToModule;
    }

    @Override
    public ICodeBaseEntry lookupResource(String resourceName) {
        resourceName = translateResourceName(resourceName);
        String packageName = getPackage(resourceName);
        String moduleName = packageToModuleMap.get(packageName);
        if(moduleName == null){
            return null;
        }
        Path resolved = root.resolve(moduleName + "/" + resourceName);
        if(Files.exists(resolved)){
            return new JrtfsCodebaseEntry(resolved, root, this);
        }
        return null;
    }

    private static String getPackage(String resourceName) {
        int lastSlash = resourceName.lastIndexOf('/');
        if(lastSlash > 0){
            return resourceName.substring(0, lastSlash);
        }
        return resourceName;
    }

    @Override
    public String getPathName() {
        return fileName;
    }

    @Override
    public int hashCode() {
        return 31 + fileName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof JrtfsCodeBase)) {
            return false;
        }
        return fileName.equals(((JrtfsCodeBase) obj).fileName);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("JrtfsCodeBase [");
        if (fileName != null) {
            builder.append("file=");
            builder.append(fileName);
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void close() {
        if(fs != null){
            try {
                fs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ICodeBaseIterator iterator() throws InterruptedException {
        return new JrtfsCodeBaseIterator();
    }

    static String fileName(Path p){
        Path name = p.getFileName();
        return name != null? name.toString() : "";
    }

    static boolean isClassFile(Path p) {
        return p.endsWith(".class") && !p.endsWith("module-info.class") && Files.isRegularFile(p);
    }

    public class JrtfsCodeBaseIterator implements ICodeBaseIterator {

        private Iterator<Path> iterator;

        public JrtfsCodeBaseIterator() {
            try {

                iterator = Files.walk(root).filter(p -> isClassFile(p)).iterator();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean hasNext() throws InterruptedException {
            return iterator.hasNext();
        }

        @Override
        public ICodeBaseEntry next() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Path next = iterator.next();
            return new JrtfsCodebaseEntry(next, root, JrtfsCodeBase.this);
        }

    }

    public static class JrtfsCodebaseEntry extends AbstractScannableCodeBaseEntry {

        private final Path path;
        private final Path root;
        private final JrtfsCodeBase codebase;

        public JrtfsCodebaseEntry(Path next, Path root, JrtfsCodeBase codebase) {
            this.path = next;
            this.root = root;
            this.codebase = codebase;
        }

        @Override
        public int getNumBytes() {
            try {
                return (int) Files.size(path);
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        public InputStream openResource() throws IOException {
            return new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
        }

        @Override
        public JrtfsCodeBase getCodeBase() {
            return codebase;
        }

        @Override
        public ClassDescriptor getClassDescriptor() throws ResourceNotFoundException, InvalidClassFileFormatException {
            return DescriptorFactory.createClassDescriptorFromResourceName(getResourceName());
        }

        @Override
        public String getRealResourceName() {
            return root.relativize(path).toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = prime + codebase.hashCode();
            result = prime * result + path.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof JrtfsCodebaseEntry)) {
                return false;
            }
            JrtfsCodebaseEntry other = (JrtfsCodebaseEntry) obj;
            if(!Objects.equals(codebase, other.codebase)){
                return false;
            }
            return Objects.equals(path, other.path);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("JrtfsCodebaseEntry [");
            if (path != null) {
                builder.append("path=");
                builder.append(path);
                builder.append(", ");
            }
            if (codebase != null) {
                builder.append("codebase=");
                builder.append(codebase);
            }
            builder.append("]");
            return builder.toString();
        }

    }


}
