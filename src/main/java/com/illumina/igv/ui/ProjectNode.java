package com.illumina.igv.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import org.broad.igv.ui.IGV;

import com.illumina.basespace.AppResult;
import com.illumina.basespace.BaseSpaceEntity;
import com.illumina.basespace.FetchParams;
import com.illumina.basespace.Project;
import com.illumina.desktop.ClientUtilities;
import com.illumina.desktop.GUIWorker;
import com.illumina.desktop.ImageProvider;
import com.illumina.desktop.tree.TreeNode;
import com.illumina.igv.BaseSpaceIGVHelper;
import com.illumina.igv.BaseSpaceResourcePath;
import com.illumina.igv.IlluminaIGVProperties;

public class ProjectNode extends BaseSpaceTreeNode<Project>
{

    public ProjectNode(Project bean)
    {
        super(bean);
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
        GUIWorker<List<AppResult>> worker = new GUIWorker<List<AppResult>>(IGV.getJFrame())
        {

            @Override
            protected List<AppResult> doInBackground() throws Exception
            {
                List<AppResult> entities = new ArrayList<AppResult>();
                entities.addAll(fetchTotal(this,(List)IGV.getInstance().getBaseSpaceSession().getAppResults(getUserObject(), getDefaultFetchParams())));
                return entities;
            }

            @Override
            protected void done()
            {
                try
                {
                    super.done();
                    if (super.getThrowable() != null)return;
                    
                    List<AppResult> list = get();
                    List<TreeNode<?>> decorators = new ArrayList<TreeNode<?>>(list.size());
                    List<TreeNode<?>> selectNodes = new ArrayList<TreeNode<?>>();
                    for (AppResult obj : list)
                    {
                        AppResultNode an = new AppResultNode((AppResult)obj); 
                        selectNodes.addAll(checkForSelectionNodeMatch(IlluminaIGVProperties.APPRESULT_ID,an));
                        decorators.add(an);
                    }
                    addChildren(tree,treeNode,decorators,selectNodes);
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
    protected void processChildrenAfterAdd(final JTree tree, final List<TreeNode<?>> children)
    {
        try
        {
            
            GUIWorker<Boolean> worker = new GUIWorker<Boolean>(IGV.getJFrame())
            {
                @Override
                protected Boolean doInBackground() throws Exception
                {
                   try
                   {
                       AppResult appresult = null;
                       for(TreeNode<?> node:children)
                       {
                           if (AppResultNode.class.isAssignableFrom(node.getClass()))
                           {
                               final AppResultNode appResultNode = (AppResultNode)node;
                               appresult = appResultNode.getUserObject();
                               appresult = IGV.getInstance().getBaseSpaceSession().getAppResult(appresult.getId().toString());
                               String sampleName = BaseSpaceIGVHelper.getDisplayNameForAppResult(appresult);
                               appResultNode.setName(sampleName);
                               DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                               model.nodeChanged(appResultNode);
                           }
                       }
                   }
                   catch(Throwable t)
                   {
                       t.printStackTrace();
                   }
                   return new Boolean(true);
                }
            };
            worker.executeAndProceed();
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }
    
   



    @Override
    protected <T extends BaseSpaceEntity> List<T>  fetchMoreResults(Class<T> type, int offset)
    {
        FetchParams params = getDefaultFetchParams();
        params.setOffSet(offset);
        List<T>rtn = new ArrayList<T>();
        if (AppResult.class.isAssignableFrom(type))
        {
            List<AppResult> appresults = IGV.getInstance().getBaseSpaceSession().getAppResults(getUserObject(), params);
            rtn.addAll((List)appresults);
        }
        return rtn;
    }


    @Override
    public void renderNode(JTree tree, JLabel label, Project value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
        label.setText("<html><b>" + value.getName() + "</b>"
                + " (id#" + value.getId() + ") Owned By [" + value.getUserOwnedBy().getName() + "]"
                + "</html>");
        label.setIcon(ImageProvider.instance().getIcon("folder_add.png"));
    }

    @Override
    public void build(BaseSpaceResourcePath path)
    {
        path.setProjectId(this.getUserObject().getId());
    }

}
