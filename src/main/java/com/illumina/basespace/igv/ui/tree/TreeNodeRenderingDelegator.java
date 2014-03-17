package com.illumina.basespace.igv.ui.tree;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

public class TreeNodeRenderingDelegator implements TreeCellRenderer
{
    private JLabel label = new JLabel("");
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus)
    {
        if (BaseSpaceTreeNode.class.isAssignableFrom(value.getClass()))
        {
            BaseSpaceTreeNode<?> node = (BaseSpaceTreeNode<?>)value;
            return node.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
       
        }
        if (value != null)label.setText(value.toString());
        return label;
     }

}
