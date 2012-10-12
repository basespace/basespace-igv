/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

/*
 * SortDialog.java
 *
 * Created on December 12, 2007, 12:55 AM
 */

package org.broad.igv.ui.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author eflakes
 */
public class SortDialog extends javax.swing.JDialog {

    private boolean isCanceled = true;

    /**
     * Creates new form SortDialog
     */
    public SortDialog(java.awt.Frame parent, boolean modal, Object[] data) {
        super(parent, modal);
        initComponents();

        if (data != null)
            setModel(data);

        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                isCanceled = false;
                setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                isCanceled = true;
                setVisible(false);
            }
        });
        setLocationRelativeTo(parent);
    }

    public boolean[] isAscending() {

        boolean[] isAscending = new boolean[3];
        isAscending[0] = acsendingRadioButton1.isSelected();
        isAscending[1] = acsendingRadioButton2.isSelected();
        isAscending[2] = acsendingRadioButton3.isSelected();
        return isAscending;
    }

    private void setModel(Object sortKeys[]) {

        if (sortKeys == null || sortKeys.length < 1)
            return;

        Object dataModel[] = new Object[sortKeys.length + 1];
        dataModel[0] = null;

        for (int i = 0; i < sortKeys.length; i++) {
            dataModel[i + 1] = sortKeys[i];
        }
        sortByComboBox.setModel(new DefaultComboBoxModel(dataModel));
        sortByComboBox.setSelectedIndex(0);

        thenBy1ComboBox.setModel(new DefaultComboBoxModel(dataModel));
        thenBy1ComboBox.setSelectedIndex(0);

        thenBy2ComboBox.setModel(new DefaultComboBoxModel(dataModel));
        thenBy2ComboBox.setSelectedIndex(0);
    }

    public String[] getSelectedSortKeys() {

        int arraySize = 0;
        List<String> selected = new ArrayList<String>();

        String item1 = ((String) sortByComboBox.getSelectedItem());
        if (item1 != null) {
            selected.add(item1);
            ++arraySize;
        }
        String item2 = ((String) thenBy1ComboBox.getSelectedItem());
        if (item2 != null) {
            selected.add(item2);
            ++arraySize;
        }
        String item3 = ((String) thenBy2ComboBox.getSelectedItem());
        if (item3 != null) {
            selected.add(item3);
            ++arraySize;
        }

        String selectedSortKeys[] = selected.toArray(new String[arraySize]);
        return selectedSortKeys;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sortOrderButtonGroup = new javax.swing.ButtonGroup();
        sortOrderButtonGroup2 = new javax.swing.ButtonGroup();
        sortOrderButtonGroup3 = new javax.swing.ButtonGroup();
        sortByComboBox = new javax.swing.JComboBox();
        thenBy1ComboBox = new javax.swing.JComboBox();
        thenBy2ComboBox = new javax.swing.JComboBox();
        acsendingRadioButton1 = new javax.swing.JRadioButton();
        descendingRadioButton1 = new javax.swing.JRadioButton();
        sortByLabel = new javax.swing.JLabel();
        thenBy1Label = new javax.swing.JLabel();
        thenBy2Label = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        descendingRadioButton2 = new javax.swing.JRadioButton();
        acsendingRadioButton2 = new javax.swing.JRadioButton();
        descendingRadioButton3 = new javax.swing.JRadioButton();
        acsendingRadioButton3 = new javax.swing.JRadioButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Sort");
        setMinimumSize(new java.awt.Dimension(330, 300));
        setName("Sort"); // NOI18N
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        getContentPane().add(sortByComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 60, 210, -1));

        getContentPane().add(thenBy1ComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 120, 210, -1));

        getContentPane().add(thenBy2ComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 180, 210, -1));

        sortOrderButtonGroup.add(acsendingRadioButton1);
        acsendingRadioButton1.setSelected(true);
        acsendingRadioButton1.setText("Ascending");
        acsendingRadioButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        acsendingRadioButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        getContentPane().add(acsendingRadioButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 60, -1, -1));

        sortOrderButtonGroup.add(descendingRadioButton1);
        descendingRadioButton1.setText("Descending");
        descendingRadioButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        descendingRadioButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        getContentPane().add(descendingRadioButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 80, -1, -1));

        sortByLabel.setBackground(new java.awt.Color(204, 204, 255));
        sortByLabel.setText("Sort by");
        getContentPane().add(sortByLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 40, -1, -1));

        thenBy1Label.setText("Then by");
        getContentPane().add(thenBy1Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, -1, -1));

        thenBy2Label.setText("Then by");
        getContentPane().add(thenBy2Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 160, -1, -1));

        okButton.setText("Ok");
        getContentPane().add(okButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 240, 90, -1));

        cancelButton.setText("Cancel");
        getContentPane().add(cancelButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 240, 90, -1));
        getContentPane().add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 50, 280, 10));
        getContentPane().add(jSeparator2, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 110, 280, 10));
        getContentPane().add(jSeparator3, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 170, 280, 10));
        getContentPane().add(jSeparator4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 288, 390, -1));

        sortOrderButtonGroup2.add(descendingRadioButton2);
        descendingRadioButton2.setText("Descending");
        descendingRadioButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        descendingRadioButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        getContentPane().add(descendingRadioButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 140, -1, -1));

        sortOrderButtonGroup2.add(acsendingRadioButton2);
        acsendingRadioButton2.setSelected(true);
        acsendingRadioButton2.setText("Ascending");
        acsendingRadioButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        acsendingRadioButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        getContentPane().add(acsendingRadioButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 120, -1, -1));

        sortOrderButtonGroup3.add(descendingRadioButton3);
        descendingRadioButton3.setText("Descending");
        descendingRadioButton3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        descendingRadioButton3.setMargin(new java.awt.Insets(0, 0, 0, 0));
        getContentPane().add(descendingRadioButton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 200, -1, -1));

        sortOrderButtonGroup3.add(acsendingRadioButton3);
        acsendingRadioButton3.setSelected(true);
        acsendingRadioButton3.setText("Ascending");
        acsendingRadioButton3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        acsendingRadioButton3.setMargin(new java.awt.Insets(0, 0, 0, 0));
        getContentPane().add(acsendingRadioButton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 180, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SortDialog(new javax.swing.JFrame(), true, null).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton acsendingRadioButton1;
    private javax.swing.JRadioButton acsendingRadioButton2;
    private javax.swing.JRadioButton acsendingRadioButton3;
    private javax.swing.JButton cancelButton;
    private javax.swing.JRadioButton descendingRadioButton1;
    private javax.swing.JRadioButton descendingRadioButton2;
    private javax.swing.JRadioButton descendingRadioButton3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JButton okButton;
    private javax.swing.JComboBox sortByComboBox;
    private javax.swing.JLabel sortByLabel;
    private javax.swing.ButtonGroup sortOrderButtonGroup;
    private javax.swing.ButtonGroup sortOrderButtonGroup2;
    private javax.swing.ButtonGroup sortOrderButtonGroup3;
    private javax.swing.JComboBox thenBy1ComboBox;
    private javax.swing.JLabel thenBy1Label;
    private javax.swing.JComboBox thenBy2ComboBox;
    private javax.swing.JLabel thenBy2Label;
    // End of variables declaration//GEN-END:variables

}
