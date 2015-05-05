package com.illumina.basespace.igv.ui.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingWorker;

import org.broad.igv.ui.IGV;

import com.illumina.basespace.entity.AppResult;
import com.illumina.basespace.entity.AppResultCompact;
import com.illumina.basespace.entity.AppSession;
import com.illumina.basespace.entity.AppSessionCompact;
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.igv.BaseSpaceConstants;
import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.BaseSpaceMain.ClientContext;
import com.illumina.basespace.igv.ui.BaseSpaceHelper;
import com.illumina.basespace.igv.ui.ImageProvider;
import com.illumina.basespace.igv.ui.ProgressReport;
import com.illumina.basespace.infrastructure.BaseSpaceException;
import com.illumina.basespace.param.FileParams;
import com.illumina.basespace.param.QueryParams;
import com.illumina.basespace.property.AppResultReference;
import com.illumina.basespace.property.Property;

public class AppSessionNode extends BaseSpaceTreeNode<AppSessionCompact>
{
    private static QueryParams params;
    private String appResultId;
    
    static
    {
        params = new QueryParams(128);
        params.setSortDir(QueryParams.DESCENDING);
        params.setSortBy("Id");
    }

    public AppSessionNode(AppSession bean,UUID clientId, ClientContext clientContext)
    {
        super(bean, clientId, clientContext);
      //  this.appResultId = appResultId;
    }
    
    public AppSessionNode(AppSessionCompact bean,UUID clientId, ClientContext clientContext)
    {
        super(bean, clientId, clientContext);
      //  this.appResultId = appResultId;
    }

    @Override
    public void loadChildrenAsynch(final JTree tree)
    {
        final BaseSpaceTreeNode<?> treeNode = this;
        BrowserDialog.instance().workInit(100);
        final FileParams fileParams = new FileParams(BaseSpaceConstants.FILE_TYPES.keySet().toArray(new String[0]), 
        		128);
        final String thisAppResultId = appResultId;
        
        SwingWorker< List<BaseSpaceTreeNode<?>>,ProgressReport> worker = new SwingWorker< List<BaseSpaceTreeNode<?>>,ProgressReport>()
        {
            /**
             * Load appresults for an appsession: /appsessions/{id}/appresults
             */
        	@Override
            protected  List<BaseSpaceTreeNode<?>> doInBackground() throws Exception
            {
                try
                {
                 
                    List<AppResultCompact> entities = new ArrayList<AppResultCompact>();
//                    List<AppResultCompact> appResultCompactList = new ArrayList<AppResultCompact>();
                    entities.addAll(Arrays.asList(BaseSpaceMain.instance().getApiClient(getClientId()).getAppResults(getBean(), params).items()));
                    
                    BrowserDialog.instance().setWorkMax(entities.size());
                    List<String>appResultIDs = new ArrayList<String>();
                    List<BaseSpaceTreeNode<?>> decorators = new ArrayList<BaseSpaceTreeNode<?>>(entities.size());
                    int count = 0;
                    for (AppResultCompact obj : entities)
                    {
                        count++;
                        if (obj.getStatus().equalsIgnoreCase(BaseSpaceConstants.STATUS_COMPLETE) && !appResultIDs.contains(obj.getId()))
                        {
                            FileCompact[]files = BaseSpaceMain.instance().getApiClient(getClientId()).getFiles(obj, fileParams).items();
                            if (files.length > 0)
                            {
                                publish(new ProgressReport(obj.getName(),count));
                                AppResultNode an = new AppResultNode(obj,getClientId(),getClientContext()); 
                                decorators.add(an);
                                appResultIDs.add(obj.getId());
                            }
                        }
                    }


                    return decorators;
                }
                catch(Throwable t)
                {
                    t.printStackTrace();
                    throw new RuntimeException("Error adding appresults",t);
                }
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
                    super.done();
                    BrowserDialog.instance().workDone();
                    List<BaseSpaceTreeNode<?>> decorators =  get();
                    addChildren(tree,treeNode,decorators);
                }
                catch(Throwable t)
                {
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

    
    @Override
    public void renderNode(JTree tree, JLabel label, AppSessionCompact value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus)
    {
        String nameToDisplay = value.getName() + " " + value.getId();
        label.setText( "<html>" + nameToDisplay + "</html>");
        label.setIcon(ImageProvider.instance().getIcon("sample.png"));
        
    }

}
