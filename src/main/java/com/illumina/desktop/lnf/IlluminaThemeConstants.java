package com.illumina.desktop.lnf;

import java.awt.Color;
import java.awt.Font;

import javax.swing.plaf.FontUIResource;

public class IlluminaThemeConstants
{
	public static class Colors
	{

		public static final Color ORANGE_GRADIENT1 = new Color(255, 226, 182);
		public static final Color ORANGE_GRADIENT2 = new Color(253, 210, 144);
		public static final Color ORANGE_GRADIENT3 = new Color(255, 199, 112);

		public static final Color BUTTON_GRADIENT1 = new Color(177, 204, 229);
		public static final Color BUTTON_GRADIENT2 = new Color(146, 186, 224);
		public static final Color BUTTON_GRADIENT3 = new Color(121, 166, 210);
		public static final Color BUTTON_GRADIENT4 = new Color(126, 169, 212);
		public static final Color BUTTON_ROLLOVER_GRADIENT1 = new Color(197, 224, 249);
		public static final Color BUTTON_ROLLOVER_GRADIENT2 = new Color(166, 206, 244);
		public static final Color BUTTON_ROLLOVER_GRADIENT3 = new Color(141, 186, 230);
		public static final Color BUTTON_ROLLOVER_GRADIENT4 = new Color(146, 189, 232);
		public static final Color BUTTON_BORDER = new Color(111, 159, 207);

		public static final Color ORANGE_LIGHT = new Color(254, 218, 163);
		public static final Color ORANGE = new Color(255, 199, 112);

		public static final Color BLUE = new Color(124, 168, 212);
		public static final Color DARK_GRAY = new Color(97, 97, 97);
		public static final Color LIGHT_GRAY = new Color(187, 187, 187);

		public static final Color DOCKING_TAB_FOCUSED_GRADIENT1 = ORANGE;
		public static final Color DOCKING_TAB_FOCUSED_GRADIENT2 = Color.WHITE;

		public static final Color DOCKING_TAB_FOCUSLOST_GRADIENT1 = LIGHT_GRAY;
		public static final Color DOCKING_TAB_FOCUSLOST_GRADIENT2 = Color.WHITE;

		public static final Color DOCKING_TAB_SELECTED_GRADIENT1 = LIGHT_GRAY;
		public static final Color DOCKING_TAB_SELECTED_GRADIENT2 = Color.WHITE;

		public static final Color DOCKING_TAB_GRADIENT1 = new Color(245, 245, 245);
		public static final Color DOCKING_TAB_GRADIENT2 = new Color(245, 245, 245);

		public static final Color DOCKING_TEXT = Color.BLACK;
		public static final Color DOCKING_TEXT_SELECTED = Color.BLACK;
		public static final Color DOCKING_TEXT_FOCUSED = Color.BLACK;
		public static final Color DOCKING_TEXT_FOCUSLOST = Color.BLACK;

		public static final Color DOCKING_BORDER = LIGHT_GRAY;
		public static final Color DOCKING_BORDER_SELECTED = LIGHT_GRAY;
		public static final Color DOCKING_BORDER_FOCUSED = ORANGE_GRADIENT3;
		public static final Color DOCKING_BORDER_FOCUSLOST = LIGHT_GRAY;

		public static final Color DOCKING_STACK_BORDER = ORANGE_GRADIENT3;
		public static final Color DOCKING_SELECTION_BORDER = ORANGE_GRADIENT3;

	}

	public static class Fonts
	{
		public static final FontUIResource NORMAL = new FontUIResource("Arial", Font.PLAIN, 11);
		public static final FontUIResource BOLD = new FontUIResource("Arial", Font.BOLD, 11);
	}
}
