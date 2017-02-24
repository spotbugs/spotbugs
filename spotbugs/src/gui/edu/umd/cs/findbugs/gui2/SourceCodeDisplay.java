/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.awt.Color;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nonnull;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.charsets.SourceCharset;
import edu.umd.cs.findbugs.sourceViewer.JavaSourceDocument;

public final class SourceCodeDisplay implements Runnable {
    final MainFrame frame;

    private static final Color MAIN_HIGHLIGHT = new Color(1f, 1f, 0.5f);

    private static final Color MAIN_HIGHLIGHT_MORE = MAIN_HIGHLIGHT.brighter();

    private static final Color ALTERNATIVE_HIGHLIGHT = new Color(0.86f, 0.90f, 1.0f);

    private static final Color FOUND_HIGHLIGHT = new Color(0.75f, 0.75f, 1f);

    static final Document SOURCE_NOT_RELEVANT = new DefaultStyledDocument();

    public JavaSourceDocument myDocument;

    private int currentChar = -1; // for find

    private final Map<String, SoftReference<JavaSourceDocument>> map = new HashMap<String, SoftReference<JavaSourceDocument>>();

    SourceCodeDisplay(MainFrame frame) {
        this.frame = frame;
        Thread t = new Thread(this, "Source code display thread");
        t.setDaemon(true);
        t.start();
    }


    static class DisplayMe {
        public DisplayMe(BugInstance bug, SourceLineAnnotation source) {
            this.bug = bug;
            this.source = source;
        }
        final BugInstance bug;
        final SourceLineAnnotation source;
    }

    final BlockingQueue<DisplayMe> queue = new   LinkedBlockingQueue<DisplayMe>();

    public  void displaySource(BugInstance bug, SourceLineAnnotation source) {
        queue.add(new DisplayMe(bug, source));
    }

    public void clearCache() {
        map.clear();
    }

    @Nonnull
    private JavaSourceDocument getDocument(SourceLineAnnotation source) {
        try {
            SourceFile sourceFile = frame.getProject().getSourceFinder().findSourceFile(source);
            String fullFileName = sourceFile.getFullFileName();
            SoftReference<JavaSourceDocument> resultReference = map.get(fullFileName);
            JavaSourceDocument result = null;
            if (resultReference != null) {
                result = resultReference.get();
            }
            if (result != null) {
                return result;
            }
            try {
                InputStream in = sourceFile.getInputStream();
                result = new JavaSourceDocument(source.getClassName(), SourceCharset.bufferedReader(in), sourceFile);
            } catch (Exception e) {
                result = JavaSourceDocument.UNKNOWNSOURCE;
                Debug.println(e); // e.printStackTrace();
            }
            map.put(fullFileName, new SoftReference<JavaSourceDocument>(result));
            return result;
        } catch (Exception e) {
            Debug.println(e); // e.printStackTrace();
            return JavaSourceDocument.UNKNOWNSOURCE;

        }
    }

    @Override
    public void run() {
        while (true) {
            DisplayMe display;
            try {
                display = queue.take();
            } catch (InterruptedException e1) {
                assert false;
                Debug.println(e1);
                continue;
            }
            BugInstance myBug = display.bug;
            SourceLineAnnotation mySourceLine = display.source;

            if (myBug == null || mySourceLine == null) {
                frame.clearSourcePane();
                continue;
            }

            try {
                JavaSourceDocument src = getDocument(mySourceLine);
                this.myDocument = src;
                src.getHighlightInformation().clear();
                String primaryKind = mySourceLine.getDescription();
                // Display myBug and mySourceLine
                for (Iterator<BugAnnotation> i = myBug.annotationIterator(); i.hasNext();) {
                    BugAnnotation annotation = i.next();
                    if (annotation instanceof SourceLineAnnotation) {
                        SourceLineAnnotation sourceAnnotation = (SourceLineAnnotation) annotation;
                        if (sourceAnnotation == mySourceLine) {
                            continue;
                        }
                        if (sourceAnnotation.getDescription().equals(primaryKind)) {
                            highlight(src, sourceAnnotation, MAIN_HIGHLIGHT_MORE);
                        } else {
                            highlight(src, sourceAnnotation, ALTERNATIVE_HIGHLIGHT);
                        }
                    }
                }
                highlight(src, mySourceLine, MAIN_HIGHLIGHT);
                javax.swing.SwingUtilities.invokeLater(new DisplayBug(src, myBug, mySourceLine));
            } catch (Exception e) {
                Debug.println(e); // e.printStackTrace();
            }
        }
    }


    private final class DisplayBug implements Runnable {

        private final SourceLineAnnotation mySourceLine;
        private final JavaSourceDocument src;
        private final BugInstance myBug;


        private DisplayBug(JavaSourceDocument src, BugInstance myBug, SourceLineAnnotation mySourceLine) {
            this.mySourceLine = mySourceLine;
            this.src = src;
            this.myBug = myBug;
        }

        @Override
        public void run() {
            frame.getSourceCodeTextPane().setEditorKit(src.getEditorKit());
            StyledDocument document = src.getDocument();
            frame.getSourceCodeTextPane().setDocument(document);
            String sourceFile = mySourceLine.getSourceFile();
            if (sourceFile == null || "<Unknown>".equals(sourceFile)) {
                sourceFile = mySourceLine.getSimpleClassName();
            }
            int startLine = mySourceLine.getStartLine();
            int endLine = mySourceLine.getEndLine();
            frame.setSourceTab(sourceFile + " in " + mySourceLine.getPackageName(), myBug);

            int originLine = (startLine + endLine) / 2;
            LinkedList<Integer> otherLines = new LinkedList<Integer>();
            // show(frame.getSourceCodeTextPane(), document,
            // thisSource);
            for (Iterator<BugAnnotation> i = myBug.annotationIterator(); i.hasNext();) {
                BugAnnotation annotation = i.next();
                if (annotation instanceof SourceLineAnnotation) {
                    SourceLineAnnotation sourceAnnotation = (SourceLineAnnotation) annotation;
                    if (sourceAnnotation != mySourceLine) {
                        // show(frame.getSourceCodeTextPane(),
                        // document, sourceAnnotation);
                        int otherLine = sourceAnnotation.getStartLine();
                        if (otherLine > originLine) {
                            otherLine = sourceAnnotation.getEndLine();
                        }
                        otherLines.add(otherLine);
                    }
                }
            }

            if (startLine >= 0 && endLine >= 0) {
                frame.getSourceCodeTextPane().scrollLinesToVisible(startLine, endLine, otherLines);
            }
        }
    }

    /**
     * @param src
     * @param sourceAnnotation
     */
    private void highlight(JavaSourceDocument src, SourceLineAnnotation sourceAnnotation, Color color) {

        int startLine = sourceAnnotation.getStartLine();
        if (startLine == -1) {
            return;
        }
        String sourceFile = sourceAnnotation.getSourcePath();
        String sourceFile2 = src.getSourceFile().getFullFileName();
        if (!java.io.File.separator.equals(String.valueOf(SourceLineAnnotation.CANONICAL_PACKAGE_SEPARATOR))) {
            sourceFile2 = sourceFile2.replace(java.io.File.separatorChar, SourceLineAnnotation.CANONICAL_PACKAGE_SEPARATOR);
        }
        if (!sourceFile2.endsWith(sourceFile)) {
            return;
        }
        src.getHighlightInformation().setHighlight(startLine, sourceAnnotation.getEndLine(), color);
    }

    public void foundItem(int lineNum) {
        myDocument.getHighlightInformation().updateFoundLineNum(lineNum);
        myDocument.getHighlightInformation().setHighlight(lineNum, FOUND_HIGHLIGHT);
        frame.getSourceCodeTextPane().scrollLineToVisible(lineNum);
        frame.getSourceCodeTextPane().updateUI();
    }

    private int search(JavaSourceDocument document, String target, int start, Boolean backwards) {
        if (document == null) {
            return -1;
        }

        String docContent = null;
        try {
            StyledDocument document2 = document.getDocument();
            if (document2 == null) {
                return -1;
            }
            docContent = document2.getText(0, document2.getLength());
        } catch (BadLocationException ble) {
            System.out.println("Bad location exception");
        } catch (NullPointerException npe) {
            return -1;
        }
        if (docContent == null) {
            return -1;
        }
        int targetLen = target.length();
        int sourceLen = docContent.length();
        if (targetLen > sourceLen) {
            return -1;
        } else if (backwards) {
            for (int i = start; i >= 0; i--) {
                if (docContent.substring(i, i + targetLen).equals(target)) {
                    return i;
                }
            }
            for (int i = (sourceLen - targetLen); i > start; i--) {
                if (docContent.substring(i, i + targetLen).equals(target)) {
                    return i;
                }
            }
            return -1;
        } else {
            for (int i = start; i <= (sourceLen - targetLen); i++) {
                if (docContent.substring(i, i + targetLen).equals(target)) {
                    return i;
                }
            }
            for (int i = 0; i < start; i++) {
                if (docContent.substring(i, i + targetLen).equals(target)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private int charToLineNum(int charNum) {
        if (charNum == -1) {
            return -1;
        }
        try {
            for (int i = 1; true; i++) {
                if (frame.getSourceCodeTextPane().getLineOffset(i) > charNum) {
                    return i - 1;
                } else if (frame.getSourceCodeTextPane().getLineOffset(i) == -1) {
                    return -1;
                }
            }
        } catch (BadLocationException ble) {
            return -1;
        }
    }

    public int find(String target) {
        currentChar = search(myDocument, target, 0, false);
        // System.out.println(currentChar);
        // System.out.println(charToLineNum(currentChar));
        return charToLineNum(currentChar);
    }

    public int findNext(String target) {
        currentChar = search(myDocument, target, currentChar + 1, false);
        // System.out.println(currentChar);
        // System.out.println(charToLineNum(currentChar));
        return charToLineNum(currentChar);
    }

    public int findPrevious(String target) {
        currentChar = search(myDocument, target, currentChar - 1, true);
        // System.out.println(currentChar);
        // System.out.println(charToLineNum(currentChar));
        return charToLineNum(currentChar);
    }

    public void showLine(int line) {
        frame.getSourceCodeTextPane().scrollLineToVisible(line);

    }
}
