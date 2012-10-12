package com.illumina.desktop;

import java.util.EventListener;
import java.util.EventObject;


/**
 * 
 * @author bking
 *
 */
public interface BusyDialogCancelListener extends EventListener
{
   public void onCancel(EventObject e);
}
