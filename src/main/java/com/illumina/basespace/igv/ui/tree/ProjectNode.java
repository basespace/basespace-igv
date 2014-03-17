package com.illumina.basespace.igv.ui.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingWorker;

import org.broad.igv.ui.IGV;

import com.illumina.basespace.entity.AppResultCompact;
import com.illumina.basespace.entity.AppSession;
import com.illumina.basespace.entity.AppSessionCompact;
import com.illumina.basespace.entity.Project;
import com.illumina.basespace.entity.ProjectCompact;
import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.BaseSpaceMain.ClientContext;
import com.illumina.basespace.igv.ui.BaseSpaceHelper;
import com.illumina.basespace.igv.ui.ImageProvider;
import com.illumina.basespace.igv.ui.ProgressReport;
import com.illumina.basespace.param.QueryParams;
import com.illumina.basespace.property.ProjectReference;
import com.illumina.basespace.property.Property;
import com.illumina.basespace.util.TypeHelper;

public class ProjectNode extends BaseSpaceTreeNode<Project>
{
    private int maxResults = 128;
    private JPopupMenu menu;
    private static final String INPUT_PROJECT = "Input.project-id";
    private static final String OUTPUT_PROJECT = "Output.projects";
    
    private static QueryParams params;
    static
    {
        params = new QueryParams();
        params.setSortDir(QueryParams.DESCENDING);
        params.setSortBy("Id");
    }
    
    
    public ProjectNode(Project bean,UUID clientId,ClientContext clientContext)
    {
        super(bean,clientId,clientContext);
    }
    
    public JPopupMenu getPopupMenu()
    {
        if (menu != null)return menu;
        
        menu = new JPopupMenu();
        JMenuItem mi = new JMenuItem("Set Maximum Results");
        mi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String result = JOptionPane.showInputDialog("Enter max results from API", String.valueOf(maxResults));
                try
                {
                    maxResults = Integer.parseInt(result);
                }
                catch(NumberFormatException nfe)
                {
                    
                }
            }
        });
        menu.add(mi);
        return menu;
    }
    
    abstract class AppSessionWorker extends SwingWorker<List<BaseSpaceTreeNode<?>>,ProgressReport>
    {
        public void publishProgress(ProgressReport... chunks)
        {
            super.publish(chunks);
        }
    }
    
    
    public void loadChildrenAsynch(final JTree tree)
    {
        final BaseSpaceTreeNode<?> treeNode = this;
        BrowserDialog.instance().workInit(100);
        
        AppSessionWorker worker = new AppSessionWorker()
        {

            @Override
            protected List<BaseSpaceTreeNode<?>> doInBackground() throws Exception
            {
                List<BaseSpaceTreeNode<?>> decorators = new ArrayList<BaseSpaceTreeNode<?>>(64);
                decorators.addAll(getAppSessionNodes(this));
                return decorators;
            }

            @Override
            protected void process(List<ProgressReport> chunks)
            {
                BrowserDialog.instance().workProgress(chunks);
            }


            @Override
            protected void done()
            {
                try
                {
                    List<BaseSpaceTreeNode<?>> decorators = get();
                    addChildren(tree,treeNode,decorators);
                }
                catch(Throwable t)
                {
                    BrowserDialog.instance().workDone();
                    BaseSpaceHelper.showErrorDialog(IGV.getMainFrame(),t);
                }
                finally
                {
                    BrowserDialog.instance().workDone();
                }
                        
            }
        };
        BrowserDialog.instance().workStart();
        worker.execute();    
    }
    
    protected  List<BaseSpaceTreeNode<?>>getAppSessionNodes(final AppSessionWorker worker)
    {
        List<AppSession> appSessions = new ArrayList<AppSession>();
        List<AppSessionCompact> appSessionCompactList = new ArrayList<AppSessionCompact>();
        if (getConfig().getAppSessionId() != null)
        {
            AppSession fullAppSession = BaseSpaceMain.instance().getApiClient(getClientId()).getAppSession(getConfig().getAppSessionId()).get();
            Property[]props = fullAppSession.properties();
            for(Property prop:props)
            {
                if (prop.getType().equalsIgnoreCase(TypeHelper.PROPERTY_PROJECT_ARRAY) && prop.getName().equalsIgnoreCase(OUTPUT_PROJECT) )
                {
                    ProjectReference projectRef = (ProjectReference)prop;
                    if (projectRef.getContent().getId().equals(getBean().getId()))
                    {
                        worker.publishProgress(new ProgressReport(fullAppSession.getName()));
                        appSessions.add(fullAppSession);
                    }
                }
            }
        }
        else
        {  
            List<AppResultCompact> appResults = new ArrayList<AppResultCompact>();
            
            params.setLimit(maxResults);
            appResults.addAll(Arrays.asList(BaseSpaceMain.instance().getApiClient(getClientId()).getAppResults(getBean(), params).items()));
            appSessionCompactList.addAll(Arrays.asList(BaseSpaceMain.instance().getApiClient(getClientId()).getAppSessions(getConfig().getProjectId(), params).items()));
            List<String>appSessionIds = new ArrayList<String>(512);
            BrowserDialog.instance().setWorkMax( appResults.size());
        }
       

		List<BaseSpaceTreeNode<?>> decorators = new ArrayList<BaseSpaceTreeNode<?>>(appSessions.size());

		for (AppSessionCompact obj : appSessionCompactList)
        {
            AppSessionNode an = new AppSessionNode(obj,getClientId(),getClientContext()); 
            decorators.add(an);
        }
        
        return decorators;
    }
    
    private boolean isIdInArray(ProjectCompact[] projectCompact, String id) {
		for(ProjectCompact p: projectCompact){
			if(p.getId().equals(id))
				return true;
		}
		return false;
	}
    


    @Override
    public void renderNode(JTree tree, JLabel label, Project value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
        label.setText("<html><b>" + value.getName() + "</b>"
                + " Owned By " + value.getUserOwnedBy().getName() + "</html>");
        label.setIcon(ImageProvider.instance().getIcon("org.png"));
    }
 
    
}



