package com.illumina.igv.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import org.broad.igv.ui.IGV;

import com.illumina.basespace.AppResult;
import com.illumina.basespace.BaseSpaceEntity;
import com.illumina.basespace.File;
import com.illumina.basespace.FileFetchParams;
import com.illumina.desktop.ClientUtilities;
import com.illumina.desktop.GUIWorker;
import com.illumina.desktop.ImageProvider;
import com.illumina.desktop.tree.TreeNode;
import com.illumina.igv.BaseSpaceResourcePath;
import com.illumina.igv.IlluminaIGVProperties;

public class AppResultNode extends BaseSpaceTreeNode<AppResult>
{
    public AppResultNode(AppResult bean)
    {
        super(bean);
        setName(bean.getName() + " (" + bean.getId().toString() + ")");
    }

    private JTree tree;
    public JTree getTree()
    {
        return tree;
    }

    

    @Override
    public boolean loadAsynch()
    {
        return true;
    }



    @Override
    public void loadChildrenAsynch(final JTree tree)
    {
        final BaseSpaceTreeNode<?> treeNode = this;
        GUIWorker<List<File>> worker = new GUIWorker<List<File>>(IGV.getJFrame())
        {
            @Override
            protected List<File> doInBackground() throws Exception
            {
                return fetchTotal(this,(List) IGV.getInstance().getBaseSpaceSession().getFiles(getUserObject(), getDefaultFileFetchParams()));
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected void done()
            {
                try
                {
                    try
                    {
                        super.done();
                        if (super.getThrowable() != null)return;
                    }
                    catch(Throwable t)
                    {
                        return;
                    }
                    
                    List<TreeNode<?>> selectNodes = new ArrayList<TreeNode<?>>();
                    List<FileNode>fileNodes = getFileNodes(get());
                    for(FileNode fileNode:fileNodes)
                    {
                        selectNodes.addAll(checkForSelectionNodeMatch(IlluminaIGVProperties.FILE_ID,fileNode));
                    }
                    Collections.sort(selectNodes,new FileNodeSorter());
                    addChildren(tree,treeNode,(List)fileNodes,selectNodes);
                    for(TreeNode treeNode:selectNodes)
                    {
                        FileNode fileNode = (FileNode)treeNode;
                        fileNode.loadTrack();
                    }
                    IlluminaIGVProperties.instance().remove(IlluminaIGVProperties.FILE_ID);
                   
                }
                catch(Throwable t)
                {
                    ClientUtilities.showErrorDialog(IGV.getJFrame(),t);
                }
            }
        };
        worker.executeAndWait();

    }

    
    
    
    @Override
    protected <T extends BaseSpaceEntity> List<T>  fetchMoreResults(Class<T> type, int offset)
    {
        FileFetchParams params = getDefaultFileFetchParams();
        params.setOffSet(offset);
        List<T>rtn = new ArrayList<T>();
        List<File> files = IGV.getInstance().getBaseSpaceSession().getFiles(getUserObject(), params);
        rtn.addAll((List)files);
        return rtn;
    }

    @Override
    public List<? extends TreeNode<?>> getChildren()
    {
        return null;
    }

    @Override
    public void renderNode(JTree tree, JLabel label, AppResult value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
        String nameToDisplay = "<b>" + getName() + "</b>";
        label.setText( "<html>" + nameToDisplay + "</html>");
        label.setIcon(ImageProvider.instance().getIcon("chart_curve.png"));
        
    }

    @Override
    public void build(BaseSpaceResourcePath path)
    {
        path.setAppResultId(this.getUserObject().getId());
    }



}
