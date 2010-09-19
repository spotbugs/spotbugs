/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tomás Pollak
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
package de.tobject.findbugs.actions.test;

import org.eclipse.swt.widgets.FileDialog;

import de.tobject.findbugs.actions.SaveXmlAction;

/**
 * Test subclass of SaveXmlAction that overrides the opening of the FileDialog
 * for testing purposes.
 * 
 * @author Tomás Pollak
 */
public class SaveXMLActionTestSubclass extends SaveXmlAction {
    private final String filePath;

    public SaveXMLActionTestSubclass(String filePath) {
        this.filePath = filePath;
    }

    @Override
    protected String openFileDialog(FileDialog dialog) {
        return filePath;
    }

}
