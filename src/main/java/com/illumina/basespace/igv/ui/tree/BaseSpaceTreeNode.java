package com.illumina.basespace.igv.ui.tree;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import java.util.UUID;

import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;

import com.illumina.basespace.igv.BaseSpaceConfiguration;
import com.illumina.basespace.igv.BaseSpaceMain.ClientContext;
import com.illumina.basespace.igv.ui.BaseSpaceHelper;




/**
 * 
 * @author bking
 *
 * @param <T>
 */
public abstract class BaseSpaceTreeNode<T>extends DefaultMutableTreeNode implements TreeCellRenderer,NodeRenderer<T>
{
    private UUID clientId;
    
    private JLabel renderingLabel = new JLabel();
    private ClientContext clientContext;
    public BaseSpaceTreeNode(T bean,UUID clientId, ClientContext clientContext )
    {
        super(bean);
        this.clientId = clientId;
        this.clientContext = clientContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getUserObject()
    {
        return (T)super.getUserObject();
    }

    public List<? extends BaseSpaceTreeNode<?>> getChildren()
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
    
    public void loadChildrenAsynch(JTree tree)
    {
        
    }
    
    protected void addChildren(JTree tree,BaseSpaceTreeNode<?> selectedNode,List<BaseSpaceTreeNode<?>> children)
    {
        try
        {
            selectedNode.removeAllChildren();
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            if (children != null && children.size() > 0)
            {
                for (BaseSpaceTreeNode<?> child : children)
                {
                    if (child.hasChildren())
                    {
                        BaseSpaceHelper.addStubNode(tree, child);
                    }
                    selectedNode.add(child);
                }
            }
            model.nodeStructureChanged(selectedNode);
           
        }
        catch(Throwable t)
        {
            throw new RuntimeException("Error adding node children to tree",t);
        }
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

    public UUID getClientId()
    {
        return clientId;
    }

    public BaseSpaceConfiguration getConfig()
    {
        return this.clientContext.getConfig();
    }
    
    protected ClientContext getClientContext()
    {
        return clientContext;
    }
    
    

}
