package com.illumina.desktop.tree;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import com.illumina.desktop.ClientUtilities;


/**
 * 
 * @author bking
 *
 * @param <T>
 */
public abstract class TreeNode<T>extends DefaultMutableTreeNode implements TreeCellRenderer,NodeRenderer<T>
{
    private JLabel renderingLabel = new JLabel();
    public TreeNode(T bean)
    {
        super(bean);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getUserObject()
    {
        return (T)super.getUserObject();
    }

    public List<? extends TreeNode<?>> getChildren()
    {
        return null;
    }
    
    public boolean hasChildren()
    {
        return true;
    }

    public JPopupMenu getPopupMenu()
    {
        return null;
    }
    
    protected void selected()
    {
        
    }
    
    protected void doubleClicked()
    {
        
    }
    
    public boolean loadAsynch()
    {
        return false;
    }
    
    public void loadChildrenAsynch(JTree tree)
    {
        
    }
    
    protected void addChildren(JTree tree,TreeNode<?> selectedNode,List<TreeNode<?>> children,List<TreeNode<?>>nodesToSelect)
    {
        try
        {
            selectedNode.removeAllChildren();
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            if (children != null && children.size() > 0)
            {
                for (TreeNode<?> child : children)
                {
                    if (child.hasChildren())
                        ClientUtilities.addStubNode(tree, child);
                    selectedNode.add(child);
                }
            }
            model.nodeStructureChanged(selectedNode);
            selectNodes(tree,nodesToSelect);
            processChildrenAfterAdd(tree,children);
            processSelectedChildrenAfterAdd(tree,nodesToSelect);
            
        }
        catch(Throwable t)
        {
            throw new RuntimeException("Error adding node children to tree",t);
        }
    }
    
    protected void selectNodes(JTree tree,List<TreeNode<?>>nodesToSelect)
    {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        if (nodesToSelect != null && nodesToSelect.size() > 0)
        {
            for(TreeNode<?> node:nodesToSelect)
            {
                TreePath path = new TreePath(model.getPathToRoot(node));
                tree.setSelectionPath(path);
                tree.expandPath(path);
                tree.scrollPathToVisible(path);
            }
        }
    }
    
    protected void processChildrenAfterAdd(JTree tree,List<TreeNode<?>> children)
    {
        
    }
    protected void processSelectedChildrenAfterAdd(JTree tree,List<TreeNode<?>> children)
    {
        
    }
    
    @SuppressWarnings("unchecked")
    protected T getBean()
    {
        return (T)super.getUserObject();
    }
    
    
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus)
    {
        renderingLabel.setOpaque(true);
        renderingLabel.setBackground(selected ? UIManager.getColor("Tree.selectionBackground") : tree.getBackground());
        renderingLabel.setForeground(selected ? Color.WHITE : Color.BLACK);
        renderNode(tree,renderingLabel,getUserObject(),selected,expanded,leaf,row,hasFocus);
        return renderingLabel;
    }

}
