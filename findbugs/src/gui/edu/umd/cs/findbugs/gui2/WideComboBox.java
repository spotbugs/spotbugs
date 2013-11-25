package edu.umd.cs.findbugs.gui2;

import java.awt.Dimension;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

public class WideComboBox<E> extends JComboBox<E> {

    public WideComboBox() {
    }

    public WideComboBox(final E items[]) {
        super(items);
    }

    public WideComboBox(Vector<E> items) {
        super(items);
    }

    public WideComboBox(ComboBoxModel<E> aModel) {
        super(aModel);
    }

    private boolean layingOut = false;

    @Override
    public void doLayout() {
        try {
            layingOut = true;
            super.doLayout();
        } finally {
            layingOut = false;
        }
    }

    @Override
    public Dimension getSize() {
        Dimension dim = super.getSize();
        if (!layingOut) {
            dim.width = Math.max(dim.width, 300);
            dim.height = Math.max(dim.height, 500);
        }
        return dim;
    }
}
