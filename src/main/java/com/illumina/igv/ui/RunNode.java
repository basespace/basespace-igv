package com.illumina.igv.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTree;

import org.broad.igv.ui.IGV;

import com.illumina.basespace.BaseSpaceEntity;
import com.illumina.basespace.FetchParams;
import com.illumina.basespace.File;
import com.illumina.basespace.Run;
import com.illumina.basespace.Sample;
import com.illumina.desktop.ClientUtilities;
import com.illumina.desktop.GUIWorker;
import com.illumina.desktop.ImageProvider;
import com.illumina.desktop.tree.TreeNode;
import com.illumina.igv.BaseSpaceResourcePath;

public class RunNode extends BaseSpaceTreeNode<Run>
{
    public RunNode(Run bean)
    {
        super(bean);
    }

    @Override
    public List<? extends TreeNode<?>> getChildren()
   {
       try
       {
           GUIWorker<List<Sample>> worker = new GUIWorker<List<Sample>>(IGV.getJFrame())
           {
               @Override
               protected List<Sample> doInBackground() throws Exception
               {
                   return fetchTotal(this,IGV.getInstance().getBaseSpaceSession().getSamples(getBean(), getDefaultFetchParams()));
               }
           };
           worker.executeAndWait();
           List<Sample>list = worker.get();
           List<TreeNode<?>> decorators = new ArrayList<TreeNode<?>>(list.size());
           for (BaseSpaceEntity obj : list)
           {
               decorators.add(new SampleNode((Sample)obj));
           }
           return decorators;
       }
       catch(Throwable t)
       {
           ClientUtilities.showErrorDialog(IGV.getJFrame(),t);
           return new ArrayList<TreeNode<?>>();
       }
   }
    
    
    @Override
    protected <T extends BaseSpaceEntity> List<T> fetchMoreResults(Class<T> type, int offset)
    {
        FetchParams params = getDefaultFetchParams();
        params.setOffSet(offset);
        List<T>rtn = new ArrayList<T>();
        List<Sample> samples = IGV.getInstance().getBaseSpaceSession().getSamples(getUserObject(), params);
        rtn.addAll((List)samples);
        return rtn;
    }

    @Override
    public void renderNode(JTree tree, JLabel label, Run value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
        label.setText("<html><b>" + value.getName() + "</b>"
                + " (id#" + value.getId() + ")"
                + "</html>");
        label.setIcon(ImageProvider.instance().getIcon("run_exec.gif"));
        
    }

    @Override
    public void build(BaseSpaceResourcePath path)
    {
        // TODO Auto-generated method stub
        
    }



    @Override
    public void loadSession(BaseSpaceResourcePath path)
    {
   
    }


}
