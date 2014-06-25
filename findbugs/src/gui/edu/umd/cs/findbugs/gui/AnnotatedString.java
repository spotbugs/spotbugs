/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 Rohan Lloyd <findbugs@rohanl.com>
 * Copyright (C) 2004,2005 University of Maryland
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

/*
 * AnnotatedString.java
 *
 * Created on September 14, 2004
 */

package edu.umd.cs.findbugs.gui;

import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.L10N;
import edu.umd.cs.findbugs.SystemProperties;

/**
 * Class to handle Strings annotated with embedded mnemonics
 *
 * Note: Since the human interface guidelines for Mac OS X say never to use
 * mnemonics, this class behaves as if no mnemonics are set when run on Mac OS
 * X.
 */
public class AnnotatedString {

    private static final boolean MAC_OS_X = SystemProperties.getProperty("os.name").toLowerCase().startsWith("mac os x");

    private final String myAnnotatedString;

    public AnnotatedString(String s) {
        myAnnotatedString = s;
    }

    /*
     * Return the string minus any annotation
     */
    @Override
    public String toString() {
        if (MAC_OS_X) {
            // Support annotations like "File(&F)"
            if (myAnnotatedString.matches("[^&]+\\(&\\p{Alnum}\\)")) {
                int endIndex = myAnnotatedString.length() - "(&X)".length();

                return myAnnotatedString.substring(0, endIndex);
            }

            // Support annotations like "File(&F)..."
            if (myAnnotatedString.matches("[^&]+\\(&\\p{Alnum}\\)\\.\\.\\.")) {
                int startIndex = myAnnotatedString.length() - "(&X)...".length();
                int endIndex = startIndex + "(&X)".length();

                return new StringBuilder(myAnnotatedString).delete(startIndex, endIndex).toString();
            }
        }
        return myAnnotatedString.replaceFirst("&", "");
    }

    /**
     * Return the appropriate mnemonic character for this string. If no mnemonic
     * should be displayed, KeyEvent.VK_UNDEFINED is returned.
     *
     * @return the Mnemonic character, or VK_UNDEFINED if no mnemonic should be
     *         set
     */
    public int getMnemonic() {
        int mnemonic = KeyEvent.VK_UNDEFINED;
        if (!MAC_OS_X) {
            int index = getMnemonicIndex();
            if ((index >= 0) && ((index + 1) < myAnnotatedString.length())) {
                mnemonic = Character.toUpperCase(myAnnotatedString.charAt(index + 1));
            }
        }
        return mnemonic;
    }

    /**
     * @return the index in the plain string at which the mnemonic should be
     *         displayed, or -1 if no mnemonic should be set
     */
    public int getMnemonicIndex() {
        int index = -1;
        if (!MAC_OS_X) {
            index = myAnnotatedString.indexOf('&');
            if (index + 1 >= myAnnotatedString.length()) {
                index = -1;
            }
        }
        return index;
    }

    /*
     * Some test code
     */
    private static void addButton(JFrame frame, String s) {
        AnnotatedString as = new AnnotatedString(s);
        JButton button = new JButton(as.toString());
        button.setMnemonic(as.getMnemonic());
        button.setDisplayedMnemonicIndex(as.getMnemonicIndex());
        frame.getContentPane().add(button);

        System.out.println("\"" + s + "\" \"" + as + "\" '" + as.getMnemonic() + "' " + as.getMnemonicIndex());
    }

    public static void main(String[] args) {
        // Some basic tests

        final JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());

        addButton(frame, "&File");
        addButton(frame, "S&ave As...");
        addButton(frame, "Save &As...");
        addButton(frame, "Fo&o");
        addButton(frame, "Foo");

        // Error cases - make sure we handle them sensibly
        addButton(frame, "");
        addButton(frame, "&");
        addButton(frame, "Foo&");
        addButton(frame, "Cat & Dog");
        addButton(frame, "Cat && Dog");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.pack();
                frame.setVisible(true);
            }
        });

    }

    /**
     * Localise the given AbstractButton, setting the text and optionally
     * mnemonic Note that AbstractButton includes menus and menu items.
     *
     * @param button
     *            The button to localise
     * @param key
     *            The key to look up in resource bundle
     * @param defaultString
     *            default String to use if key not found
     * @param setMnemonic
     *            whether or not to set the mnemonic. According to Sun's
     *            guidelines, default/cancel buttons should not have mnemonics
     *            but instead should use Return/Escape
     */
    public static void localiseButton(AbstractButton button, String key, String defaultString, boolean setMnemonic) {
        AnnotatedString as = new AnnotatedString(L10N.getLocalString(key, defaultString));
        button.setText(as.toString());
        int mnemonic;
        if (setMnemonic && (mnemonic = as.getMnemonic()) != KeyEvent.VK_UNDEFINED) {
            button.setMnemonic(mnemonic);
            button.setDisplayedMnemonicIndex(as.getMnemonicIndex());
        }
    }
}
