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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.PluginLoader;

/**
 * @author pugh
 */
public class SelectPluginsDialog extends JDialog {

    JButton okButton;

    JButton cancelButton;

    JButton addButton;

    JPanel center;

    Map<PluginLoader, Boolean> selectedStatus = new HashMap<PluginLoader, Boolean>();


    SelectPluginsDialog() {
        this.setModal(true);
        this.setTitle("Select FindBugs plugins");
        setLayout(new BorderLayout());
        center = new JPanel();

        BoxLayout centerLayout = new BoxLayout(center, BoxLayout.Y_AXIS);
        center.setLayout(centerLayout);
        Collection<PluginLoader> plugins = PluginLoader.getAllPlugins();
        for (final PluginLoader loader : plugins) {
            if (loader.isCorePlugin())
                continue;
            String text = loader.getPlugin().getShortDescription();
            String id = loader.getPlugin().getPluginId();
            if (text == null)
                text = id;
            final JCheckBox checkBox = new JCheckBox(text, loader.globalledEnabled());
            String longText = loader.getPlugin().getDetailedDescription();
            if (longText != null)
                checkBox.setToolTipText("<html>" + longText +"</html>");
            selectedStatus.put(loader, loader.globalledEnabled());
            checkBox.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    boolean selected = checkBox.isSelected();
                    selectedStatus.put(loader, selected);
                }
            });
            center.add(checkBox);

        }
        JScrollPane listScroller = new JScrollPane(center);

        add(listScroller, BorderLayout.CENTER);

        okButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.ok_btn", "OK"));
        cancelButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.cancel_btn", "Cancel"));
        addButton = new JButton("add");

        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.addChoosableFileFilter(new FileFilter() {

                    @Override
                    public String getDescription() {
                        return "Select FindBugs plugin jar file";
                    }

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory())
                            return true;
                        if (!f.canRead())
                            return false;
                        if (f.getName().endsWith(".jar"))
                            return true;
                        return false;
                    }
                });
                int retvalue = chooser.showDialog(SelectPluginsDialog.this, "Select");

                if (retvalue == JFileChooser.APPROVE_OPTION) {
                    File f = chooser.getSelectedFile();
                    try {
                        PluginLoader loader;
                        loader = PluginLoader.addAvailablePlugin(f.toURI().toURL());
                        String shortText = loader.getPlugin().getShortDescription();

                        JCheckBox checkBox = new JCheckBox(shortText, loader.globalledEnabled());
                        center.add(checkBox);
                        center.validate();
                        SelectPluginsDialog.this.pack();
                    } catch (MalformedURLException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (PluginException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }


                }

            }
        });
        JPanel south = new JPanel();

        south.add(addButton);
        south.add(cancelButton);
        south.add(okButton);

        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for(Map.Entry<PluginLoader,Boolean> entry : selectedStatus.entrySet()) {
                    PluginLoader loader = entry.getKey();
                loader.setGloballedEnabled(entry.getValue());
                }
                SelectPluginsDialog.this.dispose();

            }
        });
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
               SelectPluginsDialog.this.dispose();

            }
        });
        add(south, BorderLayout.SOUTH);

        this.pack();
        this.setVisible(true);

    }

    public static void main(String args[]) {
        SelectPluginsDialog w = new SelectPluginsDialog();
        w.setVisible(true);
    }

}
