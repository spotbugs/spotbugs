/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Andrei Loskutov
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
package edu.umd.cs.findbugs;

import java.io.InputStream;
import java.net.URL;

/**
 * Interface for any kind of GUI attached to the current FindBug analysis
 * 
 * @author Andrei
 */
public interface IGuiCallback {
	void showMessageDialog(String message);
	int showConfirmDialog(String message, String title, int optionType);
	String showQuestionDialog(String message, String title, String defaultValue);
    InputStream getProgressMonitorInputStream(InputStream in, int length, String msg);
    void setErrorMessage(String errorMsg);
    void displayNonmodelMessage(String title, String message);
    boolean showDocument(URL u);
}
