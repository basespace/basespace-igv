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

package org.broad.igv.ui.util;

import com.jidesoft.swing.JideButton;
import org.broad.igv.util.Filter;
import org.broad.igv.util.FilterElement;
import org.broad.igv.util.FilterElement.BooleanOperator;
import org.broad.igv.util.FilterElement.Operator;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract public class FilterComponent extends javax.swing.JPanel {

    private FilterElement filterElement;
    private FilterPane filterPane;

    public FilterComponent(FilterPane filterPane, String text, List<String> items,
                           FilterElement element) {

        initComponents();

        // Load the Item ComboBox
        this.filterPane = filterPane;
        itemComboBox.setModel(new DefaultComboBoxModel(items.toArray()));

        // Load available operators into combobox
        List<String> textForOperators = new ArrayList<String>();
        Operator[] operators = FilterElement.Operator.values();
        for (int i = 0; i < operators.length; i++) {

            // List the operators to skip
            if (operators[i].equals(Operator.GREATER_THAN_OR_EQUAL) ||
                    operators[i].equals(Operator.LESS_THAN_OR_EQUAL)) {
                continue;
            }

            textForOperators.add(operators[i].getValue());
        }
        Collections.sort(textForOperators);
        comparisonOperatorComboBox.setModel(
                new javax.swing.DefaultComboBoxModel(textForOperators.toArray()));


        // If a FilterElement was passed use it otherwise create a default one
        if (element != null) {
            filterElement = element;
            itemComboBox.setSelectedItem(filterElement.getSelectedItem());
            valueTextField.setText(filterElement.getValue());
        } else {

            String selectedItem = (String) itemComboBox.getSelectedItem();
            FilterElement.Operator selectedOperator =
                    getOperatorForText((String) comparisonOperatorComboBox.getSelectedItem());

            filterElement =
                    createFilterElement(filterPane.getFilter(),
                            selectedItem,
                            selectedOperator,
                            null,
                            null);
        }

        filterPane.getFilter().add(filterElement);
    }

    abstract public FilterElement createFilterElement(Filter filter, String item,
                                                      Operator comparisonOperator, String value, BooleanOperator booleanOperator);

    /**
     * Helper method to convert the string representation of an operator
     * to the appropriate object representation.
     */
    private FilterElement.Operator getOperatorForText(String operatorText) {

        FilterElement.Operator selected = null;

        FilterElement.Operator[] operators = FilterElement.Operator.values();
        for (FilterElement.Operator operator : operators) {

            if (operatorText.equals(operator.getValue())) {
                selected = operator;
                break;
            }
        }

        return selected;
    }

    public FilterElement getFilterElement() {
        return filterElement;
    }

    String getItem() {
        return (String) itemComboBox.getSelectedItem();
    }

    String getComparisonOperator() {
        return (String) comparisonOperatorComboBox.getSelectedItem();
    }

    String getExpectedValue() {
        return valueTextField.getText();
    }

    /**
     * Save the UI content into a non-UI version of the FilterElement
     */
    protected void save() {

        // Item
        filterElement.setSelectedItem(getItem());

        // Comparison operator
        Operator operator = getOperatorForText(getComparisonOperator());
        filterElement.setComparisonOperator(operator);

        // Value
        filterElement.setExpectedValue(getExpectedValue());
    }

    public void displayMoreButton(boolean value) {
        moreButton.setVisible(value);
    }

    protected void remove() {

        if (filterPane != null) {

            // Can not leave less than one filter element on the screen
            Component[] components = filterPane.getComponents();
            if (components.length < 2) {
                return;
            }

            // Remove the visible filter element
            filterPane.remove(this);

            // Remove the non-visual element
            filterPane.getFilter().remove(getFilterElement());

            filterPane.adjustMoreAndBooleanButtonVisibility();
            filterPane.repaint();

            // Resize window to fit the components left
            SwingUtilities.getWindowAncestor(filterPane).pack();
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        booleanButtonGroup = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        itemComboBox = new javax.swing.JComboBox();
        comparisonOperatorComboBox = new javax.swing.JComboBox();
        valueTextField = new javax.swing.JTextField();
        moreButton = new JideButton();
        removeButton = new JideButton();

        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(530, 40));
        setPreferredSize(new java.awt.Dimension(700, 40));
        setRequestFocusEnabled(false);
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 5));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setMinimumSize(new java.awt.Dimension(470, 31));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        itemComboBox.setMinimumSize(new java.awt.Dimension(50, 27));
        itemComboBox.setPreferredSize(new java.awt.Dimension(150, 27));
        itemComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemComboBoxActionPerformed(evt);
            }
        });
        itemComboBox.addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsListener() {
            public void ancestorMoved(java.awt.event.HierarchyEvent evt) {
                itemComboBoxAncestorMoved(evt);
            }

            public void ancestorResized(java.awt.event.HierarchyEvent evt) {
            }
        });
        jPanel1.add(itemComboBox);

        comparisonOperatorComboBox.setActionCommand("comparisonOperatorComboBoxChanged");
        comparisonOperatorComboBox.setMinimumSize(new java.awt.Dimension(50, 27));
        comparisonOperatorComboBox.setPreferredSize(new java.awt.Dimension(150, 27));
        comparisonOperatorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comparisonOperatorComboBoxActionPerformed(evt);
            }
        });
        jPanel1.add(comparisonOperatorComboBox);

        valueTextField.setMaximumSize(new java.awt.Dimension(32767, 20));
        valueTextField.setMinimumSize(new java.awt.Dimension(50, 27));
        valueTextField.setPreferredSize(new java.awt.Dimension(150, 27));
        valueTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                valueTextFieldActionPerformed(evt);
            }
        });
        jPanel1.add(valueTextField);

        add(jPanel1);

        moreButton.setFont(new java.awt.Font("Arial", 0, 14));
        moreButton.setText("+");
        moreButton.setContentAreaFilled(false);
        moreButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moreButton.setPreferredSize(new java.awt.Dimension(45, 27));
        moreButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moreButtonActionPerformed(evt);
            }
        });
        add(moreButton);

        removeButton.setFont(new java.awt.Font("Arial", 0, 14));
        removeButton.setText("-");
        removeButton.setContentAreaFilled(false);
        removeButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removeButton.setPreferredSize(new java.awt.Dimension(45, 27));
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });
        add(removeButton);
    }// </editor-fold>//GEN-END:initComponents

    private void valueTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_valueTextFieldActionPerformed

    }//GEN-LAST:event_valueTextFieldActionPerformed

    private void comparisonOperatorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comparisonOperatorComboBoxActionPerformed

    }//GEN-LAST:event_comparisonOperatorComboBoxActionPerformed

    private void itemComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemComboBoxActionPerformed

    }//GEN-LAST:event_itemComboBoxActionPerformed

    private void itemComboBoxAncestorMoved(java.awt.event.HierarchyEvent evt) {//GEN-FIRST:event_itemComboBoxAncestorMoved
        // TODO add your handling code here:
    }//GEN-LAST:event_itemComboBoxAncestorMoved

    private void moreButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreButtonActionPerformed

        if (filterPane.more()) {
            displayMoreButton(false);
        }
    }//GEN-LAST:event_moreButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        // TODO add your handling code here:
        remove();
    }//GEN-LAST:event_removeButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup booleanButtonGroup;
    private javax.swing.JComboBox comparisonOperatorComboBox;
    private javax.swing.JComboBox itemComboBox;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton moreButton;
    private javax.swing.JButton removeButton;
    private javax.swing.JTextField valueTextField;
    // End of variables declaration//GEN-END:variables
}
