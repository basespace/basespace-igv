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
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.entity.Reference;
import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.BaseSpaceMain.ClientContext;
import com.illumina.basespace.igv.BaseSpaceUtil;
import com.illumina.basespace.igv.ui.BaseSpaceHelper;
import com.illumina.basespace.igv.ui.ImageProvider;
import com.illumina.basespace.param.FileParams;

public class AppResultNode extends BaseSpaceTreeNode<AppResultCompact>
{
    public AppResultNode(AppResultCompact bean,UUID clientId,ClientContext clientContext)
    {
        super(bean,clientId,clientContext);
    }

    private JTree tree;
    public JTree getTree()
    {
        return tree;
    }

    @Override
    public void loadChildrenAsynch(final JTree tree)
    {
        final BaseSpaceTreeNode<?> treeNode = this;
        SwingWorker<List<FileCompact>,Object> worker = new SwingWorker<List<FileCompact>,Object>()
        {
            @Override
            protected List<FileCompact> doInBackground() throws Exception
            {
                List<FileCompact> entities = new ArrayList<FileCompact>();
                FileParams fileParams = new FileParams(new String[]{".bam",".bai",".vcf",".gz",".tbi",".bed",".gtf"},128);
                entities.addAll(Arrays.asList(BaseSpaceMain.instance().getApiClient(getClientId()).getFiles(getBean(), fileParams).items()));
                return entities;
            }

            @Override
            protected void done()
            {
                try
                {
                    super.done();
                    List<FileCompact> list = get();
                    List<BaseSpaceTreeNode<?>> decorators = new ArrayList<BaseSpaceTreeNode<?>>(list.size());
                    for (FileCompact obj : list)
                    {
                       if (obj.getName().toLowerCase().endsWith(".bam"))
                       {
                    	   FileCompact indexFile =  BaseSpaceUtil.findFile(obj.getName() + ".bai", list);
                    	   if (indexFile != null)
                    	   {
                    		   decorators.add(new FileNode(obj,indexFile,getClientId(),getClientContext()));
                    	   }
                       }
                       else if (obj.getName().toLowerCase().endsWith(".vcf"))
                       {
                    	   decorators.add(new FileNode(obj,getClientId(),getClientContext()));
                       }
                       else if (obj.getName().toLowerCase().endsWith(".vcf.gz"))
                       {
                           FileCompact indexFile = BaseSpaceUtil.findFile(obj.getName() + ".tbi", list);
                           if (indexFile != null)
                           {
                               decorators.add(new FileNode(obj,indexFile,getClientId(),getClientContext()));
                           }
                       }
                       else if (obj.getName().toLowerCase().endsWith(".bed"))
                       {
                           decorators.add(new FileNode(obj,getClientId(),getClientContext()));
                       }
                       else if (obj.getName().toLowerCase().endsWith(".bedgraph.gz"))
                       {
                           decorators.add(new FileNode(obj,getClientId(),getClientContext()));
                       }
                       else if (obj.getName().toLowerCase().endsWith(".gtf"))
                       {
                           decorators.add(new FileNode(obj,getClientId(),getClientContext()));
                       }
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
    public void renderNode(JTree tree, JLabel label, AppResultCompact value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
       
  
        //String nameToDisplay = getBean().getName() + (referencedSampleName != null?"_" + referencedSampleName:"");
        String nameToDisplay = getBean().getName();
        label.setText( "<html>" + nameToDisplay + "</html>");
        label.setIcon(ImageProvider.instance().getIcon("appresult.png"));
        
    }


}
