package edu.umd.cs.findbugs.gui2;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.apache.commons.lang.StringUtils;

import edu.umd.cs.findbugs.cloud.CloudPlugin;

public class CloudCommentsPaneSwing extends CloudCommentsPane {


    @Override
    public Dimension getPreferredSize() {
        return super.getMinimumSize();

    }

    @Override
    protected void setupLinksOrButtons() {
        signInOutLink = new JButton("Sign in");
        ((JButton)signInOutLink).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                signInOrOutClicked();
            }
        });
        cancelLink = new JButton("Cancel");
        ((JButton) cancelLink).addActionListener(new ActionListener() {
            @Override
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
            String id = _bugCollection.getCloud().getPlugin().getId();
            String thisid = plugin.getId();
            boolean selected = id.equals(thisid);
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(descriptions.get(i), selected);
            item.setToolTipText(plugin.getDetails());
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    changeCloud(plugin.getId());
                }
            });
            popup.add(item);
        }
        popup.show(signInOutLink, 0, signInOutLink.getHeight() + 5);
    }

    @Override
    protected void setSignInOutText(String buttonText) {
        ((JButton) signInOutLink).setText(StringUtils.capitalize(buttonText));
    }

}
