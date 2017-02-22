package edu.umd.cs.findbugs.gui2;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.MutableComboBoxModel;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import edu.umd.cs.findbugs.AWTEventQueueExecutor;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.util.LaunchBrowser;

public abstract class AbstractSwingGuiCallback implements IGuiCallback {
    private final AWTEventQueueExecutor bugUpdateExecutor = new AWTEventQueueExecutor();

    private final Component parent;

    public AbstractSwingGuiCallback(Component parent) {
        this.parent = parent;
    }

    @Override
    public ExecutorService getBugUpdateExecutor() {
        return bugUpdateExecutor;
    }

    @Override
    public void showMessageDialogAndWait(final String message) throws InterruptedException {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(parent, message);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(parent, message);
                    }
                });
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void showMessageDialog(final String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(parent, message);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(parent, message);
                }
            });
        }
    }

    @Override
    public int showConfirmDialog(String message, String title, String ok, String cancel) {
        return JOptionPane.showOptionDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new Object[] { ok, cancel }, ok);
    }

    @Override
    public InputStream getProgressMonitorInputStream(InputStream in, int length, String msg) {
        ProgressMonitorInputStream pmin = new ProgressMonitorInputStream(parent, msg, in);
        ProgressMonitor pm = pmin.getProgressMonitor();

        if (length > 0) {
            pm.setMaximum(length);
        }
        return pmin;
    }

    @Override
    public void displayNonmodelMessage(String title, String message) {
        DisplayNonmodelMessage.displayNonmodelMessage(title, message, parent, true);
    }

    @Override
    public String showQuestionDialog(String message, String title, String defaultValue) {
        return (String) JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE, null, null,
                defaultValue);
    }

    @Override
    public List<String> showForm(String message, String title, List<FormItem> items) {
        int result = showFormDialog(message, title, items);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        updateFormItemsFromGui(items);
        List<String> results = new ArrayList<String>();
        for (FormItem item : items) {
            results.add(item.getCurrentValue());
        }
        return results;
    }

    @Override
    public boolean showDocument(URL u) {
        return LaunchBrowser.showDocument(u);
    }

    @Override
    public boolean isHeadless() {
        return false;
    }

    @Override
    public void invokeInGUIThread(Runnable r) {
        SwingUtilities.invokeLater(r);
    }


    private void updateFormItemsFromGui(List<FormItem> items) {
        for (FormItem item : items) {
            JComponent field = item.getField();
            if (field instanceof JTextComponent) {
                JTextComponent textComponent = (JTextComponent) field;
                item.setCurrentValue(textComponent.getText());

            } else if (field instanceof JComboBox) {
                @SuppressWarnings("unchecked")
                JComboBox<String> box = (JComboBox<String>) field;
                String value = (String) box.getSelectedItem();
                item.setCurrentValue(value);
            }
            item.updated();
        }
        updateComboBoxes(items);
    }

    private void updateComboBoxes(List<FormItem> items) {
        for (FormItem item : items) {
            JComponent field = item.getField();
            if (field instanceof JComboBox) {
                @SuppressWarnings("unchecked")
                JComboBox<String> box = (JComboBox<String>) field;
                List<String> newPossibleValues = item.getPossibleValues();
                if (!boxModelIsSame(box, newPossibleValues)) {
                    MutableComboBoxModel<String> mmodel = (MutableComboBoxModel<String>) box.getModel();
                    replaceBoxModelValues(mmodel, newPossibleValues);
                    mmodel.setSelectedItem(item.getCurrentValue());
                }
            }
        }
    }

    private void replaceBoxModelValues(MutableComboBoxModel<String> mmodel, List<String> newPossibleValues) {
        try {
            while (mmodel.getSize() > 0) {
                mmodel.removeElementAt(0);
            }
        } catch (Exception e) {
            // ignore weird index out of bounds exceptions
        }
        for (String value : newPossibleValues) {
            mmodel.addElement(value);
        }
    }

    private boolean boxModelIsSame(JComboBox<String> box, List<String> newPossibleValues) {
        boolean same = true;
        if (box.getModel().getSize() != newPossibleValues.size()) {
            same = false;
        } else {
            for (int i = 0; i < box.getModel().getSize(); i++) {
                if (!box.getModel().getElementAt(i).equals(newPossibleValues.get(i))) {
                    same = false;
                    break;
                }
            }
        }
        return same;
    }

    private int showFormDialog(String message, String title, final List<FormItem> items) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 2;
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel(message), gbc);
        gbc.gridwidth = 1;

        for (FormItem item : items) {
            item.setItems(items);
            gbc.gridy++;
            panel.add(new JLabel(item.getLabel()), gbc);
            String defaultValue = item.getDefaultValue();
            if (item.getPossibleValues() != null) {
                JComboBox<?> box = createComboBox(items, item);
                panel.add(box, gbc);

            } else {
                JTextField field = createTextField(items, item);
                panel.add(field, gbc);
            }
        }

        return JOptionPane.showConfirmDialog(parent, panel, title, JOptionPane.OK_CANCEL_OPTION);
    }

    private JTextField createTextField(final List<FormItem> items, FormItem item) {
        String defaultValue = item.getDefaultValue();
        JTextField field = (item.isPassword() ? new JPasswordField() : new JTextField());
        if (defaultValue != null) {
            field.setText(defaultValue);
        }
        item.setField(field);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            private void changed() {
                updateFormItemsFromGui(items);
            }
        });
        return field;
    }

    private JComboBox<String> createComboBox(final List<FormItem> items, FormItem item) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        JComboBox<String> box = new JComboBox<>(model);
        item.setField(box);
        for (String possibleValue : item.getPossibleValues()) {
            model.addElement(possibleValue);
        }
        String defaultValue = item.getDefaultValue();
        if (defaultValue == null) {
            model.setSelectedItem(model.getElementAt(0));
        } else {
            model.setSelectedItem(defaultValue);
        }
        box.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateFormItemsFromGui(items);
            }
        });
        return box;
    }
}
