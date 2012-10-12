package com.illumina.desktop.lnf;

import bibliothek.extension.gui.dock.theme.EclipseTheme;
import bibliothek.gui.dock.themes.ColorScheme;
import bibliothek.gui.dock.util.DockProperties;
import bibliothek.gui.dock.util.PropertyKey;
import bibliothek.gui.dock.util.property.DynamicPropertyFactory;

public class IlluminaDockingTheme extends EclipseTheme
{
	public static final PropertyKey<ColorScheme> ILLUMINA_COLOR_SCHEME = new PropertyKey<ColorScheme>(
			"dock.ui.EclipseTheme.ColorScheme", new DynamicPropertyFactory<ColorScheme>()
			{
				public ColorScheme getDefault(PropertyKey<ColorScheme> key, DockProperties properties)
				{
					return new IlluminaDockingColorScheme();
				}
			}, true);

	public IlluminaDockingTheme()
	{
		super();
		setColorSchemeKey(ILLUMINA_COLOR_SCHEME);
	}
}
