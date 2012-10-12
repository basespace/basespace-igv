package com.illumina.desktop.lnf;




import bibliothek.gui.dock.themes.basic.BasicColorScheme;

public class IlluminaDockingColorScheme extends BasicColorScheme
{
	public IlluminaDockingColorScheme()
	{
		updateUI();
	}

	@Override
	public void updateUI()
	{
		super.updateUI();

	   
		setColor("stack.tab.border", IlluminaThemeConstants.Colors.DOCKING_BORDER);
		setColor("stack.tab.border.selected", IlluminaThemeConstants.Colors.DOCKING_BORDER_SELECTED);
		setColor("stack.tab.border.selected.focused", IlluminaThemeConstants.Colors.DOCKING_BORDER_FOCUSED);
		setColor("stack.tab.border.selected.focuslost", IlluminaThemeConstants.Colors.DOCKING_BORDER_FOCUSLOST);
		
		
		setColor("stack.tab.top", IlluminaThemeConstants.Colors.DOCKING_TAB_GRADIENT1);
		setColor("stack.tab.top.selected",IlluminaThemeConstants.Colors.DOCKING_TAB_SELECTED_GRADIENT1);
		setColor("stack.tab.top.selected.focused",IlluminaThemeConstants.Colors.ORANGE_GRADIENT1);
		setColor("stack.tab.top.selected.focuslost", IlluminaThemeConstants.Colors.DOCKING_TAB_FOCUSLOST_GRADIENT1);

		setColor("stack.tab.bottom",  IlluminaThemeConstants.Colors.DOCKING_TAB_GRADIENT2);
		setColor("stack.tab.bottom.selected",IlluminaThemeConstants.Colors.DOCKING_TAB_SELECTED_GRADIENT2);
		setColor("stack.tab.bottom.selected.focused",IlluminaThemeConstants.Colors.ORANGE_GRADIENT3);
		setColor("stack.tab.bottom.selected.focuslost", IlluminaThemeConstants.Colors.DOCKING_TAB_FOCUSLOST_GRADIENT2);

		setColor("stack.tab.text", IlluminaThemeConstants.Colors.DOCKING_TEXT);
		setColor("stack.tab.text.selected", IlluminaThemeConstants.Colors.DOCKING_TEXT_SELECTED);
		setColor("stack.tab.text.selected.focused", IlluminaThemeConstants.Colors.DOCKING_TEXT_FOCUSED);
		setColor("stack.tab.text.selected.focuslost", IlluminaThemeConstants.Colors.DOCKING_TEXT_FOCUSLOST);

		setColor("stack.border",  IlluminaThemeConstants.Colors.DOCKING_STACK_BORDER);
		
		
		setColor("selection.border", IlluminaThemeConstants.Colors.DOCKING_SELECTION_BORDER);
		
	}
}
