package com.illumina.basespace.igv.ui.tree;

import javax.swing.JLabel;
import javax.swing.JTree;

public interface NodeRenderer<T>
{
    public void renderNode(JTree tree,JLabel label,T value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus);
}
