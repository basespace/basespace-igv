package com.illumina.igv.ui;

import java.util.Comparator;

import com.illumina.desktop.tree.TreeNode;

public class FileNodeSorter implements Comparator<TreeNode<?>>
{

    @Override
    public int compare(TreeNode<?> node1, TreeNode<?> node2)
    {
        if (!FileNode.class.isAssignableFrom(node1.getClass())
               || !FileNode.class.isAssignableFrom(node2.getClass()))
        {
            return 0;
        }
        FileNode fileNode1 = (FileNode)node1;
        FileNode fileNode2 = (FileNode)node2;
        return compare(fileNode1,fileNode2);
    }

    private int compare(FileNode node1,FileNode node2)
    {
        switch(node1.getFileType())
        {
            case BAM:
                switch(node2.getFileType())
                {
                    case VCF:
                        return 1;
                    default:
                        return 0;
                }
            case VCF:
                switch(node2.getFileType())
                {
                    case BAM:
                        return -1;
                    default:
                        return 0;
                }   
           default:
               return 0;
        }
    }
    
}
