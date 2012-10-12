package com.illumina.desktop.tree;

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
        if (TreeNode.class.isAssignableFrom(value.getClass()))
        {
            TreeNode<?> node = (TreeNode<?>)value;
            return node.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
       
        }
        return label;
     }

}
