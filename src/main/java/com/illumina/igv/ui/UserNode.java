package com.illumina.igv.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTree;

import org.broad.igv.ui.IGV;

import com.illumina.basespace.BaseSpaceEntity;
import com.illumina.basespace.FetchParams;
import com.illumina.basespace.Project;
import com.illumina.basespace.User;
import com.illumina.desktop.ClientUtilities;
import com.illumina.desktop.GUIWorker;
import com.illumina.desktop.ImageProvider;
import com.illumina.desktop.tree.TreeNode;
import com.illumina.igv.BaseSpaceResourcePath;
import com.illumina.igv.IlluminaIGVProperties;

public class UserNode extends BaseSpaceTreeNode<User>
{

    public UserNode(User bean)
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
        GUIWorker<List<Project>> worker = new GUIWorker<List<Project>>(IGV.getJFrame())
        {

            @Override
            protected List<Project> doInBackground() throws Exception
            {
                List<Project> entities = new ArrayList<Project>();
                if (IlluminaIGVProperties.instance().getProperty(IlluminaIGVProperties.PROJECT_ID) != null)
                {
                    String projectId = (String)IlluminaIGVProperties.instance().get(IlluminaIGVProperties.PROJECT_ID);
                    entities.add(IGV.getInstance().getBaseSpaceSession().getProject(projectId));
                }
                else
                {
                    entities.addAll(fetchTotal(this,(List)IGV.getInstance().getBaseSpaceSession().getProjects(getUserObject(), getDefaultFetchParams())));
                }
                return entities;
            }

            @Override
            protected void done()
            {
                try
                {
                    super.done();
                    if (super.getThrowable() != null)return;
                    List<Project> list = get();
                    List<TreeNode<?>> decorators = new ArrayList<TreeNode<?>>(list.size());
                    List<TreeNode<?>> selectNodes = new ArrayList<TreeNode<?>>();
                    for (Project project : list)
                    {
                        ProjectNode pn = new ProjectNode(project);
                        selectNodes.addAll(checkForSelectionNodeMatch(IlluminaIGVProperties.PROJECT_ID,pn));
                        decorators.add(pn);
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
    protected <T extends BaseSpaceEntity> List<T> fetchMoreResults( Class<T>type,int offset)
    {
        FetchParams params = getDefaultFetchParams();
        params.setOffSet(offset);
        List<T>rtn = new ArrayList<T>();
        if (Project.class.isAssignableFrom(type))
        {
            List<Project> projects = IGV.getInstance().getBaseSpaceSession().getProjects(getUserObject(), params);
            rtn.addAll((List)projects);
        }
        return rtn;
    }


    @Override
    public List<? extends TreeNode<?>> getChildren()
    {
        return null;
    }

    @Override
    public void renderNode(JTree tree, JLabel label, User value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
        label.setText(value.getName()
                + " (id#" + value.getId() + ") [" + value.getEmail() + "]");
        label.setIcon(ImageProvider.instance().getIcon("user.png"));
        
    }

    @Override
    public void build(BaseSpaceResourcePath path)
    {
        path.setUserId(this.getUserObject().getId());
    }




}
