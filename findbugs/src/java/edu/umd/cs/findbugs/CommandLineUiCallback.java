/*
 * Contributions to FindBugs
 * Copyright (C) 2009, University of Maryland
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

import javax.swing.JOptionPane;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

/**
 * Implementation of the UI callback for command line sessions.
 * @author andy.st
 */
public class CommandLineUiCallback implements IGuiCallback {
   public void showMessageDialog(String message) {
    System.out.println(message);
  }

  public int showConfirmDialog(String message, String title, int optionType) {
    String confirmStr = "Yes (Y) or No (N)?";
    switch (optionType) {
      case JOptionPane.YES_NO_CANCEL_OPTION:
        confirmStr = "Yes (Y), No (N), or Cancel (C)?";
        break;
      case JOptionPane.OK_CANCEL_OPTION:
        confirmStr = "Ok (Y) or Cancel (C)?";
        break;
      case JOptionPane.DEFAULT_OPTION:
        confirmStr = "Press Y to continue.";
        break;
    }
    System.out.println(String.format("Confirmation required: %s\n\t%s\n\t%s", title, message, confirmStr));
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String answer = null;
    while (true) {
      try {
        answer = br.readLine();
      } catch (IOException ioe) {
        throw new IllegalArgumentException("IO error trying to read System.in!");
      }
      int response = parseAnswer(answer);
      if (response == 0) {
        System.out.println(String.format("\t%s", confirmStr));
      } else {
        return response;
      }
    }
  }

  private int parseAnswer(String answer) {
    if (null == answer || answer.length() == 0) {
      return 0;
    }
    char option = answer.toLowerCase(Locale.ENGLISH).charAt(0);
    switch (option) {
      case 'o':
        return JOptionPane.OK_OPTION;
      case 'y':
        return JOptionPane.YES_OPTION;
      case 'n':
        return JOptionPane.NO_OPTION;
      case 'c':
        return JOptionPane.CANCEL_OPTION;
      default:
        return -1;
    }
  }

   public InputStream getProgressMonitorInputStream(InputStream in, int length, String msg) {
    return in;
  }

   public void setErrorMessage(String errorMsg) {
    System.err.println(errorMsg);
  }

  public void displayNonmodelMessage(String title, String message) {
    System.out.println(String.format("Message: %s\n%s", title, message));
  }

/* (non-Javadoc)
 * @see edu.umd.cs.findbugs.IGuiCallback#showQuestionDialog(java.lang.String, java.lang.String, java.lang.String)
 */
public String showQuestionDialog(String message, String title, String defaultValue) {
	throw new UnsupportedOperationException();
}

/* (non-Javadoc)
 * @see edu.umd.cs.findbugs.IGuiCallback#showDocument(java.net.URL)
 */
public boolean showDocument(URL u) {
	return false;
}
}
