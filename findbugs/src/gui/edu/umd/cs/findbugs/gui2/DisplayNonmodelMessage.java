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

package edu.umd.cs.findbugs.gui2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * @author pwilliam
 */
public class DisplayNonmodelMessage {

    public static void main(String args[]) {
        displayNonmodelMessage("Hello", "The quick brown fox jumped over the lazy dog", null, false);
    }

    static JFrame messageFrame;

    static JTextArea messageTextArea;

    static Font sourceFont = new Font("Monospaced", Font.PLAIN, (int) Driver.getFontSize());

    static void setMessageFrame(JFrame messageFrame) {
        DisplayNonmodelMessage.messageFrame = messageFrame;
    }

    public static void displayNonmodelMessage(String title, String message, @CheckForNull Component centerOver, boolean onTop) {
        boolean positionWindow = false;
        if (messageFrame == null) {
            positionWindow = true;
            setMessageFrame(new JFrame(title));

            messageTextArea = new JTextArea(40, 80);
            messageTextArea.setEditable(false);
            messageTextArea.setLineWrap(true);
            messageTextArea.setWrapStyleWord(true);
            messageTextArea.setFont(sourceFont);
            try {
                messageFrame.setIconImage(ImageIO.read(MainFrame.class.getResource("smallBuggy.png")));
            } catch (IOException e1) {
                assert true; // ignore
            }
            Container contentPane = messageFrame.getContentPane();
            contentPane.setLayout(new BorderLayout());
            JScrollPane scrollPane = new JScrollPane(messageTextArea);

            contentPane.add(scrollPane, BorderLayout.CENTER);
            messageFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    JFrame tmp = messageFrame;
                    setMessageFrame(null);
                    tmp.setVisible(false);
                    tmp.dispose();
                }
            });
        }
        messageTextArea.setText(message);
        messageFrame.setTitle(title);
        messageFrame.pack();
        if (positionWindow) {
            messageFrame.setLocationRelativeTo(centerOver);
        }

        messageFrame.setVisible(true);
        messageFrame.toFront();
        if (onTop) {
            messageFrame.setAlwaysOnTop(true);
            edu.umd.cs.findbugs.util.Util.runInDameonThread(clearAlwaysOnTopLater);
        }
    }

    static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            assert true;
        }
    }

    /*
    static Runnable moveToFrontLater = new Runnable() {
        @Override
        public void run() {
            sleep(5);
            SwingUtilities.invokeLater(moveToFront);
        }
    };
     */

    static Runnable clearAlwaysOnTopLater = new Runnable() {
        @Override
        public void run() {
            sleep(5);
            SwingUtilities.invokeLater(clearAlwaysOnTop);
        }
    };

    /*
    static Runnable moveToFront = new Runnable() {
        @Override
        public void run() {
            JFrame frame = messageFrame;
            if (frame != null) {
                frame.toFront();
            }
        }
    };
     */

    static Runnable clearAlwaysOnTop = new Runnable() {
        @Override
        public void run() {
            JFrame frame = messageFrame;
            if (frame != null) {
                frame.setAlwaysOnTop(false);
            }
        }
    };
}
