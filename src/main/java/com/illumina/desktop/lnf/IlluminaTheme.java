package com.illumina.desktop.lnf;

import java.awt.Color;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

import com.nilo.plaf.nimrod.NimRODTheme;

public class IlluminaTheme extends NimRODTheme
{
	private ColorUIResource primary1 = new ColorUIResource(new Color(10, 10, 10));
	private ColorUIResource primary2 = new ColorUIResource(IlluminaThemeConstants.Colors.ORANGE);
	private ColorUIResource primary3 = new ColorUIResource(new Color(30, 30, 30));
	private ColorUIResource secondary1 = new ColorUIResource(new Color(10, 10, 10));
	private ColorUIResource secondary2 = new ColorUIResource(IlluminaThemeConstants.Colors.ORANGE);
	private ColorUIResource secondary3 = new ColorUIResource(Color.WHITE);

	public ColorUIResource getPrimary1()
	{
		return primary1;
	}

	public ColorUIResource getPrimary2()
	{
		return primary2;
	}

	public ColorUIResource getPrimary3()
	{
		return primary3;
	}

	public ColorUIResource getSecondary1()
	{
		return secondary1;
	}

	public ColorUIResource getSecondary2()
	{
		return secondary2;
	}

	public ColorUIResource getSecondary3()
	{
		return secondary3;
	}

	public FontUIResource getControlTextFont()
	{
		return IlluminaThemeConstants.Fonts.NORMAL;
	}

	public FontUIResource getMenuTextFont()
	{
		return IlluminaThemeConstants.Fonts.NORMAL;
	}

	public FontUIResource getSubTextFont()
	{
		return IlluminaThemeConstants.Fonts.NORMAL;
	}

	public FontUIResource getSystemTextFont()
	{
		return IlluminaThemeConstants.Fonts.BOLD;
	}

	public FontUIResource getUserTextFont()
	{
		return IlluminaThemeConstants.Fonts.NORMAL;
	}

	public FontUIResource getWindowTitleFont()
	{
		return IlluminaThemeConstants.Fonts.BOLD;
	}

}
