/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2026, University of Maryland
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.text.similarity.LevenshteinDistance;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Provides auto-completion functionality for the class name search filter in the GUI.
 * Displays matching class names and simple names dynamically as the user types.
 */
public class ClassSearchAutoComplete {

    private static final int MAX_SUGGESTIONS = 15;
    private static final int POPUP_PREFERRED_WIDTH = 300;
    private static final int POPUP_PREFERRED_HEIGHT = 180;
    private static final int MAX_FUZZY_DISTANCE = 2;

    private final JTextField textField;
    private final Supplier<Collection<String>> candidateSupplier;

    private final JPopupMenu popupMenu;
    private final JList<String> suggestionList;
    private final DefaultListModel<String> listModel;

    private boolean isUpdatingFromSuggestion = false;

    /**
     * Creates and attaches auto-completion support to the given text field.
     *
     * @param textField the search text field
     * @param candidateSupplier supplier providing available candidate class names
     */
    public ClassSearchAutoComplete(JTextField textField, Supplier<Collection<String>> candidateSupplier) {
        this.textField = textField;
        this.candidateSupplier = candidateSupplier;

        this.listModel = new DefaultListModel<>();
        this.suggestionList = new JList<>(listModel);
        this.suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.suggestionList.setFocusable(false);

        this.popupMenu = new JPopupMenu();
        this.popupMenu.setFocusable(false);
        this.popupMenu.setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setPreferredSize(new Dimension(POPUP_PREFERRED_WIDTH, POPUP_PREFERRED_HEIGHT));
        this.popupMenu.add(scrollPane, BorderLayout.CENTER);

        initListeners();
    }

    /**
     * Attaches auto-completion support configured with the MainFrame's bug collection.
     *
     * @param textField the search text field
     * @param mainFrame the main frame instance
     * @return the initialized ClassSearchAutoComplete instance
     */
    public static ClassSearchAutoComplete attach(JTextField textField, MainFrame mainFrame) {
        return new ClassSearchAutoComplete(textField, () -> getAvailableClassNames(mainFrame));
    }

    /**
     * Extracts all unique class names (full qualified and simple names) from the bug collection.
     *
     * @param mainFrame the main frame instance
     * @return set of available class names
     */
    public static Set<String> getAvailableClassNames(MainFrame mainFrame) {
        Set<String> classNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (mainFrame == null) {
            return classNames;
        }
        BugCollection bugCollection = mainFrame.getBugCollection();
        if (bugCollection == null) {
            return classNames;
        }

        for (BugInstance bug : bugCollection.getCollection()) {
            ClassAnnotation primaryClass = bug.getPrimaryClass();
            if (primaryClass != null && primaryClass.getClassName() != null) {
                addClassNameVariants(classNames, primaryClass.getClassName());
            }
            for (java.util.Iterator<edu.umd.cs.findbugs.BugAnnotation> it = bug.annotationIterator(); it.hasNext();) {
                edu.umd.cs.findbugs.BugAnnotation annotation = it.next();
                if (annotation instanceof ClassAnnotation) {
                    ClassAnnotation ca = (ClassAnnotation) annotation;
                    if (ca.getClassName() != null) {
                        addClassNameVariants(classNames, ca.getClassName());
                    }
                }
            }
        }
        return classNames;
    }

    private static void addClassNameVariants(Set<String> classNames, String rawClassName) {
        String dottedName = ClassName.toDottedClassName(rawClassName);
        if (!dottedName.isEmpty()) {
            classNames.add(dottedName);
            String simpleName = ClassName.extractSimpleName(dottedName);
            if (!simpleName.isEmpty() && !simpleName.equals(dottedName)) {
                classNames.add(simpleName);
            }
        }
    }

    private void initListeners() {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onDocumentChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onDocumentChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onDocumentChanged();
            }
        });

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }
        });

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hidePopup();
            }
        });

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    applySelectedSuggestion();
                }
            }
        });
    }

    private void onDocumentChanged() {
        if (isUpdatingFromSuggestion) {
            return;
        }
        SwingUtilities.invokeLater(this::updateSuggestions);
    }

    private void handleKeyPressed(KeyEvent e) {
        if (!popupMenu.isVisible()) {
            if (e.getKeyCode() == KeyEvent.VK_DOWN && !listModel.isEmpty()) {
                showPopup();
                suggestionList.setSelectedIndex(0);
                e.consume();
            }
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN:
                int nextIndex = suggestionList.getSelectedIndex() + 1;
                if (nextIndex < listModel.getSize()) {
                    suggestionList.setSelectedIndex(nextIndex);
                    suggestionList.ensureIndexIsVisible(nextIndex);
                }
                e.consume();
                break;
            case KeyEvent.VK_UP:
                int prevIndex = suggestionList.getSelectedIndex() - 1;
                if (prevIndex >= 0) {
                    suggestionList.setSelectedIndex(prevIndex);
                    suggestionList.ensureIndexIsVisible(prevIndex);
                }
                e.consume();
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_TAB:
                if (suggestionList.getSelectedIndex() != -1) {
                    applySelectedSuggestion();
                    e.consume();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                hidePopup();
                e.consume();
                break;
            default:
                break;
        }
    }

    private void updateSuggestions() {
        String text = textField.getText();
        int caretPos = textField.getCaretPosition();
        int[] range = findCurrentTokenRange(text, caretPos);
        String currentToken = text.substring(range[0], range[1]).trim();

        if (currentToken.isEmpty()) {
            hidePopup();
            return;
        }

        Collection<String> candidates = candidateSupplier.get();
        List<String> matches = computeSuggestions(currentToken, candidates, MAX_SUGGESTIONS);

        if (matches.isEmpty()) {
            hidePopup();
            return;
        }

        listModel.clear();
        for (String match : matches) {
            listModel.addElement(match);
        }
        suggestionList.setSelectedIndex(0);

        showPopup();
    }

    private void showPopup() {
        if (!textField.isShowing()) {
            return;
        }
        popupMenu.show(textField, 0, textField.getHeight());
    }

    private void hidePopup() {
        if (popupMenu.isVisible()) {
            popupMenu.setVisible(false);
        }
    }

    private void applySelectedSuggestion() {
        String selectedValue = suggestionList.getSelectedValue();
        if (selectedValue == null) {
            return;
        }

        isUpdatingFromSuggestion = true;
        try {
            String text = textField.getText();
            int caretPos = textField.getCaretPosition();
            int[] range = findCurrentTokenRange(text, caretPos);
            String replacement = replaceTokenAtRange(text, range[0], range[1], selectedValue);
            textField.setText(replacement);
            int newCaretPos = range[0] + selectedValue.length();
            if (newCaretPos <= textField.getText().length()) {
                textField.setCaretPosition(newCaretPos);
            }
        } finally {
            isUpdatingFromSuggestion = false;
            hidePopup();
        }
    }

    /**
     * Finds the start and end indices of the token surrounding the given caret position.
     * Tokens are separated by whitespace, comma, or colon.
     *
     * @param text the entire input string
     * @param caretPos current caret position
     * @return an int array of size 2 containing [startIndex, endIndex]
     */
    public static int[] findCurrentTokenRange(String text, int caretPos) {
        if (text == null || text.isEmpty()) {
            return new int[] { 0, 0 };
        }
        int clampedPos = Math.max(0, Math.min(caretPos, text.length()));

        int start = clampedPos;
        while (start > 0 && !isDelimiter(text.charAt(start - 1))) {
            start--;
        }

        int end = clampedPos;
        while (end < text.length() && !isDelimiter(text.charAt(end))) {
            end++;
        }

        return new int[] { start, end };
    }

    private static boolean isDelimiter(char c) {
        return c == ' ' || c == '\t' || c == ',' || c == ':';
    }

    /**
     * Replaces the substring from start to end with the given replacement string.
     *
     * @param text original text
     * @param start start index
     * @param end end index
     * @param replacement replacement string
     * @return updated string
     */
    public static String replaceTokenAtRange(String text, int start, int end, String replacement) {
        if (text == null) {
            return replacement;
        }
        int safeStart = Math.max(0, Math.min(start, text.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, text.length()));
        return text.substring(0, safeStart) + replacement + text.substring(safeEnd);
    }

    /**
     * Computes suggestions for the given token from the candidate collection.
     * Suggestions are prioritized: prefix matches &gt; substring matches &gt; fuzzy matches.
     *
     * @param token current query token
     * @param candidates available class name candidates
     * @param maxResults maximum number of suggestions to return
     * @return ordered list of suggestions
     */
    public static List<String> computeSuggestions(String token, Collection<String> candidates, int maxResults) {
        if (token == null || token.trim().isEmpty() || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        String lowerToken = token.trim().toLowerCase(Locale.ENGLISH);
        List<String> prefixMatches = new ArrayList<>();
        List<String> substringMatches = new ArrayList<>();
        List<String> fuzzyMatches = new ArrayList<>();

        LevenshteinDistance ld = new LevenshteinDistance(MAX_FUZZY_DISTANCE);

        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            String lowerCandidate = candidate.toLowerCase(Locale.ENGLISH);

            if (lowerCandidate.startsWith(lowerToken)) {
                prefixMatches.add(candidate);
            } else if (lowerCandidate.contains(lowerToken)) {
                substringMatches.add(candidate);
            } else {
                // Check simple class name or full candidate for fuzzy matching
                String simpleName = ClassName.extractSimpleName(candidate).toLowerCase(Locale.ENGLISH);
                Integer dist = ld.apply(simpleName, lowerToken);
                if (dist != null && dist != -1) {
                    fuzzyMatches.add(candidate);
                } else if (candidate.length() <= lowerToken.length() + 3) {
                    Integer fullDist = ld.apply(lowerCandidate, lowerToken);
                    if (fullDist != null && fullDist != -1) {
                        fuzzyMatches.add(candidate);
                    }
                }
            }
        }

        Collections.sort(prefixMatches, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(substringMatches, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(fuzzyMatches, String.CASE_INSENSITIVE_ORDER);

        List<String> result = new ArrayList<>();
        for (String match : prefixMatches) {
            if (result.size() >= maxResults) {
                break;
            }
            result.add(match);
        }
        for (String match : substringMatches) {
            if (result.size() >= maxResults) {
                break;
            }
            if (!result.contains(match)) {
                result.add(match);
            }
        }
        for (String match : fuzzyMatches) {
            if (result.size() >= maxResults) {
                break;
            }
            if (!result.contains(match)) {
                result.add(match);
            }
        }

        return result;
    }
}
