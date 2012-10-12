package com.illumina.igv.ui;

import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import com.illumina.basespace.BaseSpaceEntity;
import com.illumina.basespace.File;
import com.illumina.desktop.ImageProvider;
import com.illumina.igv.BaseSpaceResourcePath;
import com.illumina.igv.BaseSpaceTrackLoader;

public class FileNode extends BaseSpaceTreeNode<File>
{
    private JPopupMenu popup;
    private static final DecimalFormat format = new DecimalFormat("#####0.00");
    private ViewTrackAction viewTrackAction = new ViewTrackAction();
    private Map<String,AbstractAction[]>actionsForFileType = new HashMap<String,AbstractAction[]>();
    
    public FileNode(File bean)
    {
        super(bean);
        popup = new JPopupMenu();
        popup.setOpaque(true);
        popup.setLightWeightPopupEnabled(true);
        initActions();
    }
    
    public enum FileType
    {
        BAM,
        VCF,
        BAI,
        OTHER
    }
    
    
    private void initActions()
    {
        actionsForFileType.put(".vcf",new AbstractAction[]
                {
                    viewTrackAction
                });
        actionsForFileType.put(".bam",new AbstractAction[]
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



    private File indexFile;
    void setIndexFile(File file)
    {
        this.indexFile = file;
    }
    
    

    @Override
    public JPopupMenu getPopupMenu()
    {
        int idx =  super.getBean().getName().lastIndexOf(".");
        if (idx < 0)return null;
        String fileExtLower = super.getBean().getName().toLowerCase().substring(idx);
        if(!actionsForFileType.containsKey(fileExtLower))return null;
        
        AbstractAction[]actions = actionsForFileType.get(fileExtLower);
        if (actions == null || actions.length == 0)return null;
        popup.removeAll();
        for(AbstractAction action:actions)
        {
            popup.add(action);
        }
        return popup;
    }

    @Override
    public void renderNode(JTree tree, JLabel label, File value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
        double sizeInMb = value.getSize() /(double)1048576;
        ImageIcon icon = ImageProvider.instance().getIcon("page_white_get.png");
        label.setText("<html><b>" + value.getName() + "</b> [" + format.format(sizeInMb) + "MB]</html>");
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
    
    public FileType getFileType()
    {
        try
        {
            int lastIndex = getUserObject().getName().lastIndexOf(".");
            String ext = getUserObject().getName().substring(lastIndex + 1).toUpperCase();
            return FileType.valueOf(ext);
        }
        catch(Throwable t)
        {
            return FileType.OTHER;
        }
    }
 
    public void loadTrack()
    {
        String trackName = null;
        TreeNode parent = this.getParent();
        if (parent != null && BaseSpaceTreeNode.class.isAssignableFrom(parent.getClass()))
        {
            trackName = ((BaseSpaceTreeNode<?>)parent).getName();
        }
        BaseSpaceTrackLoader.loadTrack(getBean(), indexFile, trackName);
    }

    @Override
    protected <T extends BaseSpaceEntity> List<T> fetchMoreResults(Class<T> type, int offset)
    {
        return null;
    }

    @Override
    public void build(BaseSpaceResourcePath path)
    {
        path.setFileId(this.getUserObject().getId());
    }
}