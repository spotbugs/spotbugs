package edu.umd.cs.findbugs.gui2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import edu.umd.cs.findbugs.cloud.CloudPlugin;
import org.apache.commons.lang.StringUtils;

public class CloudCommentsPaneSwing extends CloudCommentsPane {

    @Override
    protected void setupLinksOrButtons() {
        _addCommentLink = new JButton("Add comment...");
        ((JButton)_addCommentLink).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addCommentClicked();
            }
        });
        _signInOutLink = new JButton("Sign in");
        ((JButton)_signInOutLink).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                signInOrOutClicked();
            }
        });
        _changeLink = new JButton("Change...");
        ((JButton)_changeLink).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeClicked();
            }
        });
        _cancelLink = new JButton("Cancel");
        ((JButton)_cancelLink).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelClicked();
            }
        });
    }

    @Override
    protected boolean isDisabled(CloudPlugin plugin) {
        return false;
    }

    @Override
    protected void showCloudChooser(List<CloudPlugin> plugins, List<String> descriptions) {
        JPopupMenu popup = new JPopupMenu();
        for (int i = 0; i < plugins.size(); i++) {
            final CloudPlugin plugin = plugins.get(i);
            boolean selected = _bugCollection.getCloud().getPlugin().getId().equals(plugin.getId());
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(descriptions.get(i), selected);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changeCloud(plugin.getId());
                }
            });
            popup.add(item);
        }
        popup.show(_changeLink, 0, _changeLink.getHeight() + 5);
    }

    @Override
    protected void setSignInOutText(String buttonText) {
        ((JButton)_signInOutLink).setText(StringUtils.capitalize(buttonText));
    }
}
