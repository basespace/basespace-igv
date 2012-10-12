package com.illumina.desktop;

import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

import bibliothek.gui.dock.common.action.CAction;

/**
 * Defines interaction with the docking framework
 * @author bking
 *
 */
public interface DockingContentProvider
{
	/**
	 * Get the id that the docking framework will use to globally identify the dockable
	 * component
	 * @return the globally unique id for the docking component
	 */
    public String getDockingId();
    
    /**
     * Get the title text for the docking window
     * @return the title text for docking window
     */
    public String getDockingTitle();
    
    /**
     * Get the icon for the docking window
     * @return the icon for the docking window
     */
    public Icon getDockingIcon();
    
    /**
     * Get the actions for the docking windows. Return null for default actions
     * @return the actions for the docking window
     */
    public List<CAction>getDockingActions();
    
    /**
     * Get the component that will be made dockable
     * @return the component for docking
     */
    public JComponent getDockingContent();
    
 }
