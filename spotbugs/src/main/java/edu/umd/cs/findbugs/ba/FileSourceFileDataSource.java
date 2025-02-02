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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Data source for source files which are stored in the filesystem.
 */
public class FileSourceFileDataSource implements SourceFileDataSource {
    private final String fileName;
    private final URI uri;

    public FileSourceFileDataSource(String fileName) {
        this.fileName = fileName;
        this.uri = new File(fileName).toURI();
    }

    @Override
    public InputStream open() throws IOException {
        return new BufferedInputStream(new FileInputStream(fileName));
    }

    @Override
    public String getFullFileName() {
        return fileName;
    }

    @Override
    public URI getFullURI() {
        return uri;
    }

    @Override
    public long getLastModified() {
        return new File(fileName).lastModified();
    }
}
