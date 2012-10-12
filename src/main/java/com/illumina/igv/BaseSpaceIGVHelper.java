package com.illumina.igv;

import java.util.Properties;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.broad.igv.Globals;
import org.broad.igv.ui.IGV;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.illumina.basespace.AppResult;
import com.illumina.basespace.Content;
import com.illumina.basespace.File;
import com.illumina.basespace.Reference;
import com.illumina.desktop.ClientUtilities;
import com.illumina.igv.ui.BaseSpaceTreeNode;
import com.illumina.igv.ui.DockingTreePanel;
import com.illumina.igv.ui.FileNode;

public class BaseSpaceIGVHelper
{
    public static final String BASESPACE_RESOURCE = "BaseSpaceResource";
    
    public static void writeBaseSpaceSession(Document document,BaseSpaceResourceLocator locator,Element resourcesElement)
    {
        try
        {
            FileNode fileNode = findFileNode(locator.getFile());
            if (fileNode != null)
            {
                BaseSpaceResourcePath path = new BaseSpaceResourcePath();
                fileNode.buildPath(path);
                if (locator.getIndexFile() != null)
                {
                    path.setIndexFileId(locator.getIndexFile().getId());
                }
                Element bsResourceElement = document.createElement(BASESPACE_RESOURCE);
                path.setAttributes(bsResourceElement);
                resourcesElement.appendChild(bsResourceElement);
            }
        }
        catch(Throwable t)
        {
            ClientUtilities.showErrorDialog(IGV.getJFrame(), t);
        }
    }
    
    private static String version;
    public static String getVersion()
    {
        try
        {
            if (version != null)return version;
            Properties pomProperties = new Properties();
            pomProperties.load(Globals.class.getResourceAsStream("/resources/illumina.igv.properties"));
            version = pomProperties.getProperty("application.version", "1.0.0");
            return version;
        }
        catch (Throwable t)
        {
            return "Unable to determine";
        }
    }
    
    public static void readBaseSpaceSession(Element resourceElement)
    {
        try
        {
            final BaseSpaceResourcePath path = new BaseSpaceResourcePath();
            path.fromAttributes(resourceElement);
            if (!path.getUserId().equals(IGV.getInstance().getBaseSpaceSession().getCurrentUser().getId()))
            {
                throw new RuntimeException("Unable to load IGV session which was saved by a different user.");
            }
         
            File file = IGV.getInstance().getBaseSpaceSession().getFile(path.getFileId().toString());
            File indexFile = null;
            if (path.getIndexFileId() != null && path.getIndexFileId() > 0)
            {
                indexFile = IGV.getInstance().getBaseSpaceSession().getFile(path.getIndexFileId().toString());
            }
            
            BaseSpaceTrackLoader.loadTrack(file, indexFile, null);
        }
        catch(Throwable t)
        {
            ClientUtilities.showErrorDialog(IGV.getJFrame(), t);
        }
    }

    public static FileNode findFileNodeById(Long id)
    {
        JTree tree = DockingTreePanel.instance().getTree();
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) model.getRoot();
        FileNode fileNode = (FileNode) findNode(tree,null,id,node);
        return fileNode;
    }
    
    public static FileNode findFileNode(File file)
    {
        JTree tree = DockingTreePanel.instance().getTree();
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) model.getRoot();
        FileNode fileNode = (FileNode) findNode(tree,file,null,node);
        return fileNode;
    }
    
    public static DefaultMutableTreeNode findNode(JTree tree, Object userObject,Long id, DefaultMutableTreeNode startingAt)
    {
        for (int i = 0; i < startingAt.getChildCount(); i++)
        {
            if (!BaseSpaceTreeNode.class.isAssignableFrom(startingAt.getChildAt(i).getClass()))continue;
            
            BaseSpaceTreeNode<?> child = (BaseSpaceTreeNode<?>) startingAt.getChildAt(i);
            if (child.getUserObject() == null) continue;

            if (userObject != null && child.getUserObject().equals(userObject)) return child;
            if (id != null && child.getId().equals(id))return child;
            
            if (child.getChildCount() > 0)
            {
                DefaultMutableTreeNode rtn = findNode(tree, userObject, id,child);
                if (rtn != null) return rtn;
            }
        }
        return null;
    }
    
    public static String getDisplayNameForAppResult(AppResult appresult)
    {
        if (appresult.getReferences() == null || appresult.getReferences().length == 0)return appresult.getName();
        for(Reference reference:appresult.getReferences())
        {
            if (!reference.getType().equals("Sample"))continue;
            Content content = reference.getContent();
            if (content.getName()!= null && content.getName().trim().length() > 0)
            {
                return appresult.getName() + "-" + content.getName();
            }
        }
        return appresult.getName();
    }
}
