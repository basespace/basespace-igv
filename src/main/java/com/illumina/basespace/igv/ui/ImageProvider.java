package com.illumina.basespace.igv.ui;

import java.awt.Image;
import java.util.HashMap;

import javax.swing.ImageIcon;

/**
 * 
 * @author bking
 *
 */
public class ImageProvider
{
	private HashMap<String,ImageIcon> images = new HashMap<String,ImageIcon>(64);

    private static ImageProvider singletonObject;
    private ImageProvider()
    {
    }
 
    public static synchronized ImageProvider instance()
    {
        if (singletonObject == null)
            singletonObject = new ImageProvider();
        return singletonObject;
    }
	
	public ImageIcon getIcon(String name)
	{
		ImageIcon icon = images.get(name);
        if (icon == null)icon = addImage(name);
        return icon;
	}


	public Image getImage(String name)
	{
		return getIcon(name).getImage();
	}
	
    protected ImageIcon addImage(String name)
    {
    	try
    	{
    	  
	        ImageIcon icon = new ImageIcon(singletonObject.getClass().getResource("/images/" + name));
	        images.put(name,icon);
	        return icon;
    	}
    	catch(Throwable t)
    	{
    		return null;
    	}
    }
}
