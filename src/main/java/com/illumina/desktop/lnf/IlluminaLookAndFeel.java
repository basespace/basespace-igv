package com.illumina.desktop.lnf;

import java.awt.Color;

import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;

import com.nilo.plaf.nimrod.NimRODLookAndFeel;

public class IlluminaLookAndFeel extends NimRODLookAndFeel
{
	@Override
	protected void initClassDefaults(UIDefaults table)
	{
		super.initClassDefaults(table);
		table.put("ButtonUI", IlluminaButtonUI.class.getName());
		table.put("MenuBarUI",IlluminaMenuBarUI.class.getName());
		table.put("MenuUI",IlluminaMenuUI.class.getName());
		table.put("ToolBarUI", IlluminaToolBarUI.class.getName());

	}

	@Override
	protected void initComponentDefaults(UIDefaults table)
	{
		super.initComponentDefaults(table);
		table.put("MenuItem.foreground", new ColorUIResource(Color.BLACK));
		table.put("MenuItem.selectionForeground", new ColorUIResource(Color.WHITE));
		table.put("CheckBoxMenuItem.selectionForeground", new ColorUIResource(Color.WHITE));
	}

}
