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
 * Created by JFormDesigner on Thu Aug 26 21:15:02 EDT 2010
 */

package org.broad.igv.ui.util;

import org.broad.igv.util.Utilities;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author Jim Robinson
 */
public class CheckListDialog extends JDialog {

    private boolean canceled = true;
    private Set<String> selections;
    private Set<String> nonSelections;
    private List<JCheckBox> checkBoxes = new ArrayList();

    public CheckListDialog(Frame owner, java.util.List<String> choices, Collection<String> selections, boolean selectionState) {
        super(owner);
        initComponents();
        initCheckboxes(choices, selections, selectionState);
    }

    public CheckListDialog(Dialog owner) {
        super(owner);
        initComponents();
    }

    private void initCheckboxes(java.util.List<String> tmp, Collection<String> selections, boolean selectionState) {

        // Copy list before sorting
        java.util.List<String> choices = new ArrayList(tmp);
        Collections.sort(choices, Utilities.getNumericStringComparator());

        boolean allSelected = true;
        for (String s : choices) {
            JCheckBox cb = new JCheckBox(s);
            if (selections == null) {
                cb.setSelected(!selectionState);
                allSelected = !selectionState;
            } else {
                if ((selectionState == true && selections.contains(s)) ||
                        (selectionState == false && !selections.contains(s))) {
                    cb.setSelected(true);
                } else {
                    allSelected = false;
                    cb.setSelected(false);
                }

            }
            checkboxPane.add(cb);
            checkBoxes.add(cb);
        }
        allCB.setSelected(allSelected);
        getContentPane().validate();
    }


    private void okButtonActionPerformed(ActionEvent e) {

        selections = new HashSet();
        nonSelections = new HashSet();
        for (JCheckBox cb : checkBoxes) {
            if (cb.isSelected()) {
                selections.add(cb.getText());
            } else {
                nonSelections.add(cb.getText());
            }
        }
        canceled = false;
        setVisible(false);
    }

    private void cancelButtonActionPerformed(ActionEvent e) {
        canceled = true;
        setVisible(false);
    }

    private void allCBActionPerformed(ActionEvent e) {
        for(JCheckBox cb : checkBoxes) {
            cb.setSelected(allCB.isSelected());
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPane1 = new JScrollPane();
        checkboxPane = new JPanel();
        allCB = new JCheckBox();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        Container contentPane = getContentPane();
        contentPane.setLayout(null);

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== scrollPane1 ========
                {

                    //======== checkboxPane ========
                    {
                        checkboxPane.setLayout(new BoxLayout(checkboxPane, BoxLayout.Y_AXIS));

                        //---- allCB ----
                        allCB.setText("All");
                        allCB.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                allCBActionPerformed(e);
                            }
                        });
                        checkboxPane.add(allCB);
                    }
                    scrollPane1.setViewportView(checkboxPane);
                }
                contentPanel.add(scrollPane1, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed(e);
                    }
                });
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        cancelButtonActionPerformed(e);
                    }
                });
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane);
        dialogPane.setBounds(0, 0, 468, 551);

        { // compute preferred size
            Dimension preferredSize = new Dimension();
            for(int i = 0; i < contentPane.getComponentCount(); i++) {
                Rectangle bounds = contentPane.getComponent(i).getBounds();
                preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
            }
            Insets insets = contentPane.getInsets();
            preferredSize.width += insets.right;
            preferredSize.height += insets.bottom;
            contentPane.setMinimumSize(preferredSize);
            contentPane.setPreferredSize(preferredSize);
        }
        setSize(470, 575);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JScrollPane scrollPane1;
    private JPanel checkboxPane;
    private JCheckBox allCB;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    public boolean isCanceled() {
        return canceled;
    }

    public Set<String> getSelections() {
        return selections;
    }

    public Set<String> getNonSelections() {
        return nonSelections;
    }
}
