package com.illumina.basespace.igv.ui;

import java.awt.Frame;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.broad.igv.ui.IGV;

import com.illumina.basespace.entity.FileCompact;

public class BaseSpaceHelper
{
    public static void showErrorDialog(Frame parent, Throwable t)
    {
        DetailDialog dlg = new DetailDialog(parent,true,t.getMessage(),t);
        dlg.setLocationRelativeTo(IGV.getMainFrame());
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
    
    public static FileCompact findFile(String fileName,List<FileCompact>files)
    {
        for(FileCompact file:files)
        {
            if (file.getName().equalsIgnoreCase(fileName))
            {
                return file;
            }
        }
        return null;
    }
}
