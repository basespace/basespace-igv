package com.illumina.basespace.igv.ui.tree;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
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

import com.illumina.basespace.igv.ui.BaseSpaceHelper;

/**
 * 
 * @author bking
 *
 */
public class BrowserPanel extends javax.swing.JPanel implements TreeSelectionListener,TreeExpansionListener,MouseListener
{
 
    private JScrollPane scrollPane;
    protected JTree tree;
    private JLabel lblInstruction;
   
    
    public BrowserPanel() 
    {
        setLayout(new BorderLayout(0, 0));
        
        scrollPane = new JScrollPane();
        add(scrollPane, BorderLayout.CENTER);
        
        tree = new JTree()
        {
            
            @Override
            public String getToolTipText(MouseEvent event)
            {
                if (getRowForLocation(event.getX(), event.getY()) == -1)
                    return null;
                  TreePath curPath = getPathForLocation(event.getX(), event.getY());
                  if (curPath.getLastPathComponent() instanceof FileNode)
                  {
                      FileNode fileNode = (FileNode)curPath.getLastPathComponent();
                      return fileNode.getBean().getName();
                  }
                  return null;
            }
        };
        scrollPane.setViewportView(tree);
        
        tree.addTreeSelectionListener(this);
        tree.addTreeExpansionListener(this);
        tree.addMouseListener(this);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.setToolTipText("");
        tree.setCellRenderer(new TreeNodeRenderingDelegator());
        
        lblInstruction = new JLabel();
        
        String text = "<html>To load a BaseSpace file into the IGV Track Viewer please double-click the file"
                + " or right-click the file and select 'View Track Data'</html>";
        lblInstruction.setText(text);
        
        add(lblInstruction, BorderLayout.NORTH);
        
        
        initTree();
    }
    
    public JTree getTree()
    {
        return tree;
    }
    
   
    public void addUserNode(UserNode node)
    {
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode root = (  DefaultMutableTreeNode)model.getRoot();
        root.add(node);
        BaseSpaceHelper.addStubNode(tree, node);
        
        model.nodeStructureChanged(root);
      
        //int index = root.getIndex(node);
        //model.nodesWereInserted(root, new int[]{index});
      
        //model.nodesChanged(root, new int[]{count-1});
        
        //model.nodesWereInserted(root, new int[]{count-1});
       // tree.updateUI();
       // tree.invalidate();
        //tree.revalidate();
        
        //tree.invalidate();
       // tree.updateUI();
    }
    
    protected void initTree()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
       // BaseSpaceHelper.addStubNode(tree, root);
        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        tree.collapseRow(0);
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
        Object lastComponent =  e.getNewLeadSelectionPath().getLastPathComponent();
        if (BaseSpaceTreeNode.class.isAssignableFrom(lastComponent.getClass()))
        {
            BaseSpaceTreeNode<?> selectedNode = (BaseSpaceTreeNode<?>) e.getNewLeadSelectionPath().getLastPathComponent();
            if (selectedNode == null)return;
            selectedNode.selected();
        }
     
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() == 2)
        {
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            if (path == null)return;
            tree.setSelectionPath(path);
            BaseSpaceTreeNode<?> node = (BaseSpaceTreeNode<?>)path.getLastPathComponent();
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
        if (path == null || path.getLastPathComponent() == null || !BaseSpaceTreeNode.class.isAssignableFrom(BaseSpaceTreeNode.class))
        {
            return;
        }
        tree.setSelectionPath(path);
        BaseSpaceTreeNode<?> node = (BaseSpaceTreeNode<?>)path.getLastPathComponent();
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

    @Override
    public void treeExpanded(TreeExpansionEvent e)
    {
        try
        {
            if (e.getPath() == null) return;
            if (!BaseSpaceTreeNode.class.isAssignableFrom( e.getPath().getLastPathComponent().getClass()))
            {
                return;
            }
            
            BaseSpaceTreeNode<?> selectedNode = (BaseSpaceTreeNode<?>) e.getPath().getLastPathComponent();
            selectedNode.loadChildrenAsynch(tree);
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
