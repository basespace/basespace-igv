package com.illumina.desktop.tree;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.illumina.desktop.ClientUtilities;

/**
 * 
 * @author bking
 *
 */
public class TreePanel extends javax.swing.JPanel implements TreeSelectionListener,TreeExpansionListener,MouseListener
{
 
    private JScrollPane scrollPane;
    protected JTree tree;
   
    
    public TreePanel() 
    {
        setLayout(new BorderLayout(0, 0));
        
        scrollPane = new JScrollPane();
        add(scrollPane, BorderLayout.CENTER);
        
        tree = new JTree();
        scrollPane.setViewportView(tree);
        
        tree.addTreeSelectionListener(this);
        tree.addTreeExpansionListener(this);
        tree.addMouseListener(this);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.setCellRenderer(new TreeNodeRenderingDelegator());
    }
    
    public JTree getTree()
    {
        return tree;
    }
    
    public void setRootNode(TreeNode<?> rootNode)
    {
        initTree(rootNode);
    }
    
    protected void initTree(TreeNode<?> rootNode)
    {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        ClientUtilities.addStubNode(tree, rootNode);
        root.add(rootNode);
        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
    }
    
    public DefaultMutableTreeNode getRootNode()
    {
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        return (DefaultMutableTreeNode)model.getRoot();
    }
    
    



    @Override
    public void valueChanged(TreeSelectionEvent e)
    {
        if (e.getNewLeadSelectionPath() == null || e.getNewLeadSelectionPath().getLastPathComponent() == null)return;
        TreeNode<?> selectedNode = (com.illumina.desktop.tree.TreeNode<?>) e.getNewLeadSelectionPath().getLastPathComponent();
        if (selectedNode == null)return;
        selectedNode.selected();
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() == 2)
        {
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            if (path == null)return;
            tree.setSelectionPath(path);
            TreeNode<?> node = (TreeNode<?>)path.getLastPathComponent();
            node.doubleClicked();
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
      
        
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path == null)return;
        tree.setSelectionPath(path);
        TreeNode<?> node = (TreeNode<?>)path.getLastPathComponent();
        if (node == null)return;
        
        if (e.isPopupTrigger())
        {
            JPopupMenu popupMenu = node.getPopupMenu();
            if (popupMenu == null)return;
            popupMenu.show( (JComponent)e.getSource(), e.getX(), e.getY() );
        }
        
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    
        
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
              
    }

    @SuppressWarnings("unchecked")
    @Override
    public void treeExpanded(TreeExpansionEvent e)
    {
        try
        {
            if (e.getPath() == null) return;
            //DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            if (!com.illumina.desktop.tree.TreeNode.class.isAssignableFrom( e.getPath().getLastPathComponent().getClass()))
            {
                return;
            }
            
            TreeNode<?> selectedNode = (com.illumina.desktop.tree.TreeNode<?>) e.getPath().getLastPathComponent();
       
            
            //selectedNode.removeAllChildren();
            
            if (!selectedNode.loadAsynch())
            {
                List<TreeNode<?>> children = (List<TreeNode<?>>) selectedNode.getChildren();
                selectedNode.addChildren(tree, selectedNode, children,null);
                
                
                /*
                if (children != null && children.size() > 0)
                {
                    for (TreeNode<?> child : children)
                    {
                        if (child.hasChildren())
                            ClientUtilities.addStubNode(tree, child);
                        selectedNode.add(child);
                    }
                }
                //model.nodeStructureChanged(selectedNode);
                 */
                 
            }
            else
            {
                selectedNode.loadChildrenAsynch(tree);
            }
            
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }
    
    

    @Override
    public void treeCollapsed(TreeExpansionEvent event)
    {
            
    }

}
