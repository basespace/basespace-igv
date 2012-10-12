package com.illumina.desktop;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.broad.igv.ui.IGV;

public class ClientUtilities
{
    public static void centerOnScreen(Component component)
    {
        if (component == null) return;
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle abounds = component.getBounds();
        component.setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
    }
    
    public static void centerOnFrame(JFrame frame,Component component)
    {
        if (component == null) return;
        Dimension dim = frame.getSize();
        Rectangle abounds = component.getBounds();
        component.setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
    }
    
    
    public static void showErrorDialog(JFrame parent, Throwable t)
    {
        DetailDialog dlg = new DetailDialog(parent,true,t.getMessage(),t);
        dlg.setLocationRelativeTo(IGV.getJFrame());
        dlg.setVisible(true);
    }
    
    public static void addStubNode(JTree tree, DefaultMutableTreeNode node)
    {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("Loading...");
        node.add(newNode);
        int index = node.getIndex(newNode);
        model.nodesWereInserted(node, new int[] { index });
    }
}
