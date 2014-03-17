package com.illumina.basespace.igv.ui.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.entity.File;
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.igv.BaseSpaceConstants;
import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.BaseSpaceMain.ClientContext;
import com.illumina.basespace.igv.BaseSpaceTrackLoader;
import com.illumina.basespace.igv.BaseSpaceUtil;
import com.illumina.basespace.igv.ui.ImageProvider;

public class FileNode extends BaseSpaceTreeNode<FileCompact>
{
    private JPopupMenu popup;
    private ViewTrackAction viewTrackAction = new ViewTrackAction();
    private DownloadAction downloadAction = new DownloadAction();
    private Map<String,AbstractAction[]>actionsForFileType = new HashMap<String,AbstractAction[]>();
    private FileCompact indexFile;
    private boolean showSize;

    private final static DecimalFormat format = new DecimalFormat("0.00");
    private double size = 0;
    
    public FileNode(FileCompact bean,UUID clientId,ClientContext clientContext)
    {
        super(bean,clientId,clientContext);
        popup = new JPopupMenu();
        popup.setOpaque(true);
        popup.setLightWeightPopupEnabled(true);
        initActions();
        size = bean.getSize() / BaseSpaceConstants.MB;
        
    }
    
    public FileNode(FileCompact bean,FileCompact indexFile,UUID clientId,ClientContext clientContext)
    {
    	this(bean,clientId,clientContext);
    	this.indexFile = indexFile;
    }
    
    private void initActions()
    {
        actionsForFileType.put(".vcf",new AbstractAction[]
                {
                    viewTrackAction,downloadAction
                });
        actionsForFileType.put(".vcf.gz",new AbstractAction[]
                {
                    viewTrackAction
                });        
        actionsForFileType.put(".bam",new AbstractAction[]
                {
                    viewTrackAction
                });    
        actionsForFileType.put(".bed",new AbstractAction[]
                {
                    viewTrackAction
                });   
        actionsForFileType.put(".gtf",new AbstractAction[]
                {
                    viewTrackAction,downloadAction
                });          
        actionsForFileType.put(".bedgraph.gz",new AbstractAction[]
                {
                    viewTrackAction
                });          
    }
    
    public boolean hasChildren()
    {
        return false;
    }
    
    
    
    @Override
    protected void doubleClicked()
    {
        loadTrack();
    }

    @Override
    public JPopupMenu getPopupMenu()
    {
        popup.removeAll();
        JCheckBoxMenuItem miSize = new JCheckBoxMenuItem("Show Size");
        miSize.setSelected(showSize);
        final TreeNode myNode = this;
        miSize.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                showSize = !showSize;
                JTree tree = BrowserDialog.instance().getBrowserPanel().getTree();
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.nodeChanged(myNode);
            }
            
        });
        popup.add(miSize);
        for(String ext:actionsForFileType.keySet())
        {
            if (getBean().getName().toLowerCase().endsWith(ext))
            {
                AbstractAction[]actions = actionsForFileType.get(ext);
                if (actions == null || actions.length == 0) continue;
                for(AbstractAction action:actions)
                {
                    popup.add(action);
                }
            }
        }
        return popup;
    }

    @Override
    public void renderNode(JTree tree, JLabel label, FileCompact value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
        //this.tree = tree;
        String iconName = "file.png";
        if (value.getName().toLowerCase().endsWith("vcf"))
        {
            iconName = "vcf.png";
        }
        else if (value.getName().toLowerCase().endsWith("vcf.gz"))
        {
            iconName = "gzip.png";
        }
        else if (value.getName().toLowerCase().endsWith("bam"))
        {
            iconName = "bam.png";
        }
        else if (value.getName().toLowerCase().endsWith("bed"))
        {
            iconName = "bed.png";
        }
        else if (value.getName().toLowerCase().endsWith("bedgraph.gz"))
        {
            iconName = "gzip.png";
        }
        ImageIcon icon = ImageProvider.instance().getIcon(iconName);
        label.setText("<html>" + value.getName() + (showSize?" (" + format.format(size) + " Mb)":"") + "</html>");
        label.setIcon(icon);
        
    }
    
    private class ViewTrackAction extends AbstractAction
    {
        public ViewTrackAction()
        {
            super("View Track Data",ImageProvider.instance().getIcon("graphs.png"));
            putValue(Action.SHORT_DESCRIPTION,"View Track Data");
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            loadTrack();
        }
    }
    private class DownloadAction extends AbstractAction
    {
        public DownloadAction()
        {
            super("Download",ImageProvider.instance().getIcon("Download.png"));
            putValue(Action.SHORT_DESCRIPTION,"Download");
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            download();
        }
    }
    
    public void download()
    {
       
        try
        {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose Folder Where File Will Be Downloaded");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(BrowserDialog.instance()) != JFileChooser.APPROVE_OPTION)return;
            File fullFile =  BaseSpaceMain.instance().getApiClient(getClientId()).getFile(getBean().getId()).get();
            java.io.File targetPath = new java.io.File(chooser.getSelectedFile().toString() + java.io.File.separator + fullFile.getName());
            ApiClient client = BaseSpaceMain.instance().getApiClient(getClientId());
            BaseSpaceUtil.downloadFile(client, fullFile, targetPath);
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }
    
    public void loadTrack()
    {
    	List<FileCompact> files = new ArrayList<FileCompact>();
    	files.add(getBean());
    	if (indexFile != null)files.add(indexFile);
    	BaseSpaceTrackLoader.loadTracks(getClientId(),files);
    }


    
}