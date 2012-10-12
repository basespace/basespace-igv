package com.illumina.igv.ui;


import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTree;

import org.broad.igv.ui.IGV;

import com.illumina.basespace.BaseSpaceEntity;
import com.illumina.basespace.File;
import com.illumina.basespace.FileFetchParams;
import com.illumina.basespace.Sample;
import com.illumina.desktop.ClientUtilities;
import com.illumina.desktop.GUIWorker;
import com.illumina.desktop.ImageProvider;
import com.illumina.desktop.tree.TreeNode;
import com.illumina.igv.BaseSpaceResourcePath;

public class SampleNode extends BaseSpaceTreeNode<Sample>
{

    public SampleNode(Sample bean)
    {
        super(bean);
 
    }

    
    @Override
     public List<? extends TreeNode<?>> getChildren()
    {
        try
        {
            GUIWorker<List<File>> worker = new GUIWorker<List<File>>(IGV.getJFrame())
            {
                @Override
                protected List<File> doInBackground() throws Exception
                {
                    return fetchTotal(this,IGV.getInstance().getBaseSpaceSession().getFiles(getUserObject(), getDefaultFileFetchParams()));
                }
            };
            worker.executeAndWait();
            List<File>files = worker.get();
            return getFileNodes(files);
        }
        catch(Throwable t)
        {
            ClientUtilities.showErrorDialog(IGV.getJFrame(),t);
            return new ArrayList<TreeNode<?>>();
        }
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
    public void renderNode(JTree tree, JLabel label, Sample value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
        label.setText("<html><b>" + value.getName() + "</b>"
                + " (id#" + value.getId() + ")"
                + " </html>");
        label.setIcon(ImageProvider.instance().getIcon("tube16.png"));
        
    }


    @Override
    public void build(BaseSpaceResourcePath path)
    {
     
        
    }



    @Override
    public void loadSession(BaseSpaceResourcePath path)
    {
 
    }



}
