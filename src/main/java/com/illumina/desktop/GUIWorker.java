package com.illumina.desktop;

import java.awt.EventQueue;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.swing.JFrame;
import javax.swing.SwingWorker;

/**
 * 
 * @author bking
 *
 * @param <T>
 */
public class GUIWorker<T> extends SwingWorker<T, Object> 
implements BusyDialogCancelListener
{
	private JFrame parentFrame;
	private BusyDialog busyDialog;
	private Throwable throwable;

	/**
	 * Constructs a GUIWorker
	 * @param parent the parent Frame of the busy dialog that will be displayed
	 * during a synchronous operation
	 */
	public GUIWorker(JFrame parent)
	{
		parentFrame = parent;
	}
	
	public GUIWorker()
    {
   
    }

	/**
	 * Initiates a synchronous operation, a blocking busy dialog is displayed for the
	 * duration of the operation
	 */
	public void executeAndWait()
	{
	    executeAndWait(false);
	}
	
	public void executeAndWait(boolean cancelable)
    {
        super.execute();
        showBusyDialog(cancelable);
    }

	/**
	 * Initiates an asynchronous operation, which returns immediately. Equivalent to 
	 * calling execute() on SwingWorker
	 */
	public void executeAndProceed()
	{
		super.execute();
	}

	@Override
	protected void done()
	{
		try
		{
			get();
		}
		catch (CancellationException canceled)
        {
		   
        }
		catch (Throwable t)
        {
		    throwable = t;
        }
		finally
		{
			if (busyDialog != null)
			{
			    final GUIWorker<?> thisObj = this;
			    EventQueue.invokeLater(new Runnable()
			    {
                    @Override
                    public void run()
                    {
                        try
                        {
                            busyDialog.removeBusyDialogCancelListener(thisObj);
                        }
                        catch(Throwable t)
                        {
                            
                        }
                    }
			    });
				busyDialog.setVisible(false);
				busyDialog.dispose();
			}
			if (getThrowable() != null)
			{
				if (parentFrame != null) ClientUtilities.showErrorDialog(parentFrame,getThrowable());
			}
		}
	}


	@Override
	protected void process(List<Object> chunks)
	{
		ProgressReport report = null;
		if (chunks != null && chunks.size() > 0)
		{
			if (chunks.get(0).getClass().isAssignableFrom(ProgressReport.class))
			{
				report = (ProgressReport) chunks.get(chunks.size() - 1);
			}
		}
		if (busyDialog != null && report != null)
		{
			busyDialog.receiveProgress(report.getText());
		}
		super.process(chunks);
	}

	private void showBusyDialog(boolean cancelable)
	{
		if (parentFrame != null)
		{
			busyDialog = new BusyDialog(parentFrame);
			ClientUtilities.centerOnFrame(parentFrame,busyDialog);
		}
		else
		{
		    busyDialog = new BusyDialog();
		    ClientUtilities.centerOnScreen(busyDialog);
		}
		
		busyDialog.setTitle("Please wait...");
		busyDialog.setCancelable(cancelable);
		busyDialog.addBusyDialogCancelListener(this);
		busyDialog.setModal(true);
		busyDialog.setVisible(true);
	}

	@Override
	protected T doInBackground() throws Exception
	{
		return null;
	}

	public void showProgress(ProgressReport report)
	{
	    this.publish(report);
	}
	
	@Override
	public void onCancel(EventObject e)
	{
		this.cancel(true);
	}

	/**
	 * Get the Throwable associated with the operation
	 * @return the Throwable associated with the operation or null if there is no Throwable
	 */
	public Throwable getThrowable()
	{
		return throwable;
	}

}
