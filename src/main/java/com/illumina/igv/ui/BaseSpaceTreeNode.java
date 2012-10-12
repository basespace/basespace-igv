package com.illumina.igv.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTree;

import com.illumina.basespace.BaseSpaceEntity;
import com.illumina.basespace.FetchParams;
import com.illumina.basespace.FileFetchParams;
import com.illumina.basespace.ItemListMetaData;
import com.illumina.desktop.GUIWorker;
import com.illumina.desktop.tree.TreeNode;
import com.illumina.igv.BaseSpaceResourcePath;
import com.illumina.igv.IlluminaIGVProperties;


public abstract class BaseSpaceTreeNode<T extends BaseSpaceEntity> extends TreeNode<T>
{
    private FetchParams defaultFetchParams = new FetchParams(128);
    private FileFetchParams defaultFileFetchParams = new FileFetchParams(new String[]{".bam",".bai",".vcf"});
    
    
    private static  BaseSpaceTreeNode<?>selectedNode;
    public static BaseSpaceTreeNode<?> getSelectedNode()
    {
        return selectedNode;
    }
    static void setSelectedNode(BaseSpaceTreeNode<?>node)
    {
        selectedNode = node;
    }
    
    public BaseSpaceTreeNode(T bean)
    {
        super(bean);
    }

    

    @Override
    protected void selected()
    {
        selectedNode = this;
        super.selected();
    }

    private String name;
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public void renderNode(JTree tree, JLabel label, T value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
        
    }
    
    protected FetchParams getDefaultFetchParams()
    {
        return defaultFetchParams;
    }
    
    protected FileFetchParams getDefaultFileFetchParams()
    {
        return defaultFileFetchParams;
    }


    public BaseSpaceTreeNode<?> getParentBaseSpaceNode()
    {
        if (getParent() != null && BaseSpaceTreeNode.class.isAssignableFrom(getParent().getClass()))
        {
            return (BaseSpaceTreeNode<?>)getParent();
        }
        return null;
    }
    

    public List<FileNode> getFileNodes(List<com.illumina.basespace.File>files)
    {
       List<FileNode> decorators = new ArrayList<FileNode>(files.size());
       for (com.illumina.basespace.File child : files)
       {
           int idx = child.getName().lastIndexOf(".");
           if (idx == -1)continue;
           String extension = child.getName().substring(idx + 1).toLowerCase();
           
           if (!extension.equals("bam") && !extension.equals("vcf"))
           {
               continue;
           }
           
           FileNode fileNode = new FileNode(child); 
           if (extension.equals("bam"))
           {
               String indexFileName = child.getName() + ".bai";
               com.illumina.basespace.File indexFile = findFile(indexFileName,files);
               if (indexFile != null)
               {
                   fileNode.setIndexFile(indexFile);
               }
           }
           if (extension.equals("vcf"))
           {
               String indexFileName = child.getName() + ".idx";
               com.illumina.basespace.File indexFile = findFile(indexFileName,files);
               if (indexFile != null)
               {
                   fileNode.setIndexFile(indexFile);
               }
           }
           decorators.add(fileNode);
       }
       return decorators;
    }

    protected com.illumina.basespace.File findFile(String fileName,List<com.illumina.basespace.File>files)
    {
        for(com.illumina.basespace.File file:files)
        {
            if (file.getName().equalsIgnoreCase(fileName))
            {
                return file;
            }
        }
        return null;
    }
    
    @SuppressWarnings("rawtypes")
    public BaseSpaceTreeNode<?>findChild(Class<? extends BaseSpaceEntity>userObjectClass,Long id)
    {
        for(TreeNode<?> node:this.getChildren())
        {
            BaseSpaceTreeNode bstn = (BaseSpaceTreeNode)node;
            if (userObjectClass.isAssignableFrom(bstn.getUserObject().getClass()))
            {
                BaseSpaceEntity entity = (BaseSpaceEntity)bstn.getUserObject();
                if (entity.getId() == id)return bstn;
            }
        }
        return null;
    }
    
   
    protected <T extends BaseSpaceEntity> List<T>fetchTotal(GUIWorker worker,List<T> list)
    {
        if (list == null || list.size() == 0)return list;
        ItemListMetaData metaData = list.get(0).getListMetaData();
        Class<? extends BaseSpaceEntity>type = list.get(0).getClass();
        if (list.size() >= metaData.getTotalCount())return list;
        while(list.size() < metaData.getTotalCount())
        {
            com.illumina.desktop.ProgressReport report = new com.illumina.desktop.ProgressReport("Fetching " + type.getSimpleName() + "s starting at " + list.size() + "/" + metaData.getTotalCount());
            worker.showProgress(report);
            list.addAll((Collection<? extends T>) fetchMoreResults(type,list.size()));
        }
        return list;
        
    }
    
    protected abstract <T extends BaseSpaceEntity> List<T> fetchMoreResults( Class<T>type,int offset);

   
    public Long getId()
    {
        return getUserObject().getId();
    }
    
    public void buildPath(BaseSpaceResourcePath path)
    {
        BaseSpaceTreeNode<?> node = this;
        while (node != null)
        {
            node.build(path);
            node = node.getParentBaseSpaceNode();
        }
    }
    
    protected List<BaseSpaceTreeNode>getBaseSpaceNodeChildren()
    {
        List<BaseSpaceTreeNode>rtn = new ArrayList<BaseSpaceTreeNode>();
        for(int i = 0 ; i < this.getChildCount();i++)
        {
            if (BaseSpaceTreeNode.class.isAssignableFrom(this.getChildAt(i).getClass()))
            {
                BaseSpaceTreeNode node = (BaseSpaceTreeNode)this.getChildAt(i);
                rtn.add((BaseSpaceTreeNode)node);
            }
        }
        return rtn;
    }
    

    


    public void loadSession(BaseSpaceResourcePath path)
    {
      
        /*
        this.filePath = path;
        JTree tree = DockingTreePanel.instance().getTree();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        TreePath treePath = new TreePath(model.getPathToRoot(this));
        boolean isExpanded = tree.isExpanded(treePath);
        
        
        
        System.out.println("Load session for " + this.getClass().getName() + ",expanded?" + isExpanded);
        
        if (!isExpanded)
        {
            System.out.println("Loading asynch");
            loadChildrenAsynch(DockingTreePanel.instance().getTree());
        }
        else
        {
            System.out.println("Checking expanded children");
            List<TreeNode<?>> selectNodes = new ArrayList<TreeNode<?>>();
            List<BaseSpaceTreeNode> children = getBaseSpaceNodeChildren();
            for (BaseSpaceTreeNode bsNode : children)
            {
                if (bsNode.getIdAsLong().equals(filePath.getIdFromNode(bsNode)))
                {
                    System.out.println("HAVE ID match for " + bsNode.getClass().getName());
                    selectNodes.add(bsNode);
                }
            }
            super.selectNodes(tree, selectNodes);
            for(TreeNode treeNode:selectNodes)
            {
                BaseSpaceTreeNode bsNode = (BaseSpaceTreeNode)treeNode;
                if (FileNode.class.isAssignableFrom(treeNode.getClass()))
                {
                    FileNode fileNode = (FileNode)treeNode;
                    System.out.println("Load track " + fileNode.getUserObject().getName());
                    fileNode.loadTrack();
                }
                else
                {
                    bsNode.loadSession(path);
                }
            }
        }
         */
    }
   

    @Override
    protected void processSelectedChildrenAfterAdd(JTree tree, List<TreeNode<?>> children)
    {
        
    }

    protected  List<TreeNode<?>> checkForSelectionNodeMatch(String propName,BaseSpaceTreeNode<?> treeNode)
    {  
        List<TreeNode<?>> selectNodes = new ArrayList<TreeNode<?>>();
        if (propName != null)
        {
            for(Long id:IlluminaIGVProperties.instance().getIdsForProperty(propName))
            {
                if (treeNode.getUserObject().getId().equals(id))
                {
                    
                    selectNodes.add(treeNode);
                }
            }
        }
        return selectNodes;
    }
    



    
    public abstract void build(BaseSpaceResourcePath path);
}
