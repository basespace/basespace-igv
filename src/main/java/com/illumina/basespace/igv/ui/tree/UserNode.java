package com.illumina.basespace.igv.ui.tree;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingWorker;

import org.broad.igv.ui.IGV;

import com.illumina.basespace.entity.Project;
import com.illumina.basespace.entity.User;
import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.BaseSpaceMain.ClientContext;
import com.illumina.basespace.igv.ui.BaseSpaceHelper;
import com.illumina.basespace.igv.ui.ImageProvider;


public class UserNode extends BaseSpaceTreeNode<User>
{
    private static SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    
    
    public UserNode(User bean,UUID clientId,ClientContext clientContext)
    {
        super(bean,clientId,clientContext);
    }

    
    public void loadChildrenAsynch(final JTree tree)
    {
        final BaseSpaceTreeNode<?> treeNode = this;
        SwingWorker<List<Project>,Object> worker = new SwingWorker<List<Project>,Object>()
        {

            @Override
            protected List<Project> doInBackground() throws Exception
            {
                List<Project> entities = new ArrayList<Project>(1);
                entities.add(BaseSpaceMain.instance().getApiClient(getClientId()).getProject(getConfig().getProjectId()).get());
                return entities;
            }

            @Override
            protected void done()
            {
                try
                {
                    super.done();
                    List<Project> list = get();
                    List<BaseSpaceTreeNode<?>> decorators = new ArrayList<BaseSpaceTreeNode<?>>(list.size());
                    for (Project project : list)
                    {
                        ProjectNode pn = new ProjectNode(project,getClientId(),getClientContext());
                        decorators.add(pn);
                    }
                    addChildren(tree,treeNode,decorators);
                }
                catch(Throwable t)
                {
                    BaseSpaceHelper.showErrorDialog(IGV.getMainFrame(),t);
                }
            }
            
            
        };
        worker.execute();
    }
    

    @Override
    public List<? extends BaseSpaceTreeNode<?>> getChildren()
    {
        return null;
    }

    @Override
    public void renderNode(JTree tree, JLabel label, User value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
         label.setText("Session created at " + format.format(new Date(getClientContext().getCreateTime())));
        label.setIcon(ImageProvider.instance().getIcon("user.png"));
        
    }



}
