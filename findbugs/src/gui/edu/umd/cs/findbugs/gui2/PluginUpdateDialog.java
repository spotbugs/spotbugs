package edu.umd.cs.findbugs.gui2;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.updates.PluginUpdateListener;
import edu.umd.cs.findbugs.updates.UpdateChecker;
import edu.umd.cs.findbugs.util.LaunchBrowser;

public class PluginUpdateDialog implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(PluginUpdateDialog.class.getName());

    private static final int SOFTWARE_UPDATE_DIALOG_DELAY_MS = 5000;

    public void showUpdateDialog(Collection<UpdateChecker.PluginUpdate> updates, boolean force) {
        List<UpdateChecker.PluginUpdate> sortedUpdates = new ArrayList<UpdateChecker.PluginUpdate>();
        UpdateChecker.PluginUpdate core = sortUpdates(updates, sortedUpdates);

        if (DetectorFactoryCollection.instance().getUpdateChecker()
                .updatesHaveBeenSeenBefore(sortedUpdates) && !force) {
            return;
        }

        String headline;
        if (core != null && updates.size() >= 2) {
            headline = "FindBugs and some plugins have updates";
        } else if (updates.isEmpty()) {
            headline = "FindBugs and all plugins are up to date!";
        } else if (core == null) {
            headline = "Some FindBugs plugins have updates";
        } else {
            headline = null;
        }

        final JPanel comp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        if (headline != null) {
            JLabel headlineLabel = new JLabel(headline);
            headlineLabel.setFont(headlineLabel.getFont().deriveFont(Font.BOLD, 24));
            comp.add(headlineLabel, gbc);
        }
        if (!updates.isEmpty()) {
            int i = 1;
            for (final UpdateChecker.PluginUpdate update : sortedUpdates) {
                gbc.gridy = ++i;
                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.gridwidth = 1;
                gbc.weightx = 1;
                JLabel label = createPluginLabel(update);
                comp.add(label, gbc);
                gbc.weightx = 0;
                gbc.gridx = 2;
                String url = update.getUrl();
                if (url != null && url.length() > 0) {
                    JButton button = createPluginUpdateButton(comp, update);
                    comp.add(button, gbc);
                }
                String msg = update.getMessage();
                if (msg != null && msg.length() > 0) {
                    gbc.gridx = 1;
                    gbc.gridwidth = 3;
                    gbc.weightx = 1;
                    gbc.fill = GridBagConstraints.BOTH;
                    gbc.gridy = ++i;
                    JTextPane msgpane = createMessagePane(msg);
                    comp.add(msgpane, gbc);
                }
            }
        }
        JOptionPane.showMessageDialog(null, comp, "Software Updates", JOptionPane.INFORMATION_MESSAGE);
    }

    private JTextPane createMessagePane(String msg) {
        JTextPane msgpane = new JTextPane();
        msgpane.setEditable(false);
        msgpane.setFocusable(false);
        msgpane.setText(msg);
        return msgpane;
    }

    private JLabel createPluginLabel(UpdateChecker.PluginUpdate update) {
        String name;
        if (update.getPlugin().isCorePlugin()) {
            name = "FindBugs";
        } else {
            name = update.getPlugin().getShortDescription();
        }
        JLabel label = new JLabel(MessageFormat.format(
                "<html><b>{0} {2}</b> is available<br><i><small>(currently installed: {1})",
                name, update.getPlugin().getVersion(), update.getVersion()));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, label.getFont().getSize() + 4));
        return label;
    }


    public PluginUpdateListener createListener() {
        return new MyPluginUpdateListener();
    }



    private JButton createPluginUpdateButton(final JPanel comp, final UpdateChecker.PluginUpdate update) {
        JButton button = new JButton("<html><u><font color=#0000ff>More info...");
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBackground(comp.getBackground());
        button.setToolTipText(update.getUrl());
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean failed;
                String url = update.getUrl();
                try {
                    failed = url == null || !LaunchBrowser.showDocument(new URL(url));
                } catch (MalformedURLException e1) {
                    failed = true;
                }
                if (failed) {
                    JOptionPane.showMessageDialog(comp, "Could not open URL " + url);
                }
            }
        });
        return button;
    }

    private UpdateChecker.PluginUpdate sortUpdates(Collection<UpdateChecker.PluginUpdate> updates,
            List<UpdateChecker.PluginUpdate> sorted) {
        UpdateChecker.PluginUpdate core = null;
        for (UpdateChecker.PluginUpdate update : updates) {
            if (update.getPlugin().isCorePlugin()) {
                core = update;
            } else {
                sorted.add(update);
            }
        }
        // sort by name
        Collections.sort(sorted, new Comparator<UpdateChecker.PluginUpdate>() {
            @Override
            public int compare(UpdateChecker.PluginUpdate o1, UpdateChecker.PluginUpdate o2) {
                return o1.getPlugin().getShortDescription().compareTo(o2.getPlugin().getShortDescription());
            }
        });
        // place core plugin first, if present
        if (core != null) {
            sorted.add(0, core);
        }
        return core;
    }

    private class MyPluginUpdateListener implements PluginUpdateListener {
        @Override
        public void pluginUpdateCheckComplete(final Collection<UpdateChecker.PluginUpdate> updates, final boolean force) {
            if (updates.isEmpty() && !force) {
                return;
            }

            if (force) {
                showUpdateDialogInSwingThread(updates, force);
            } else {
                // wait 5 seconds before showing dialog
                edu.umd.cs.findbugs.util.Util.runInDameonThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(SOFTWARE_UPDATE_DIALOG_DELAY_MS);
                            showUpdateDialogInSwingThread(updates, force);
                        } catch (InterruptedException e) {
                            LOGGER.log(Level.FINE, "Software update dialog thread interrupted", e);
                        }
                    }
                }, "Software Update Dialog");
            }
        }

        private void showUpdateDialogInSwingThread(final Collection<UpdateChecker.PluginUpdate> updates, final boolean force) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    showUpdateDialog(updates, force);
                }
            });
        }
    }
}