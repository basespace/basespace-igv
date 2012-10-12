package com.illumina.igv.ui;

import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

import bibliothek.gui.dock.common.action.CAction;

import com.illumina.desktop.DockingContentProvider;
import com.illumina.desktop.ImageProvider;
import com.illumina.desktop.tree.TreePanel;

public class DockingTreePanel extends TreePanel implements DockingContentProvider
{
    public static final String DOCKING_ID="BaseSpace";

    private static DockingTreePanel instance;
    
    public static synchronized DockingTreePanel instance()
    {
        if (instance == null)
        {
            instance = new DockingTreePanel();
        }
        return instance;
    }
    
    private DockingTreePanel()
    {
        super();
    }

    @Override
    public String getDockingId()
    {
        return DOCKING_ID;
    }

    @Override
    public String getDockingTitle()
    {
        return "BaseSpace Session";
    }

    @Override
    public Icon getDockingIcon()
    {
        return ImageProvider.instance().getIcon("igv-icon-16px.png");
    }

    @Override
    public List<CAction> getDockingActions()
    {
        return null;
    }

    @Override
    public JComponent getDockingContent()
    {
        return this;
    }
}
