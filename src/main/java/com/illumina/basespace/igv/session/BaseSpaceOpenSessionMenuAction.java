package com.illumina.basespace.igv.session;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.broad.igv.PreferenceManager;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.UIConstants;
import org.broad.igv.ui.action.OpenSessionMenuAction;
import org.broad.igv.ui.util.FileDialogUtils;
import org.broad.igv.util.FileUtils;

public class BaseSpaceOpenSessionMenuAction extends OpenSessionMenuAction
{
    public String sessionFile = null;
    public boolean autoload = false;
    public IGV mainFrame;

    public BaseSpaceOpenSessionMenuAction(String label, int mnemonic, IGV mainFrame)
    {
        super(label, mnemonic, mainFrame);
        this.mainFrame = mainFrame;
        autoload = true;

    }

    public BaseSpaceOpenSessionMenuAction(String label, String sessionFile, IGV mainFrame)
    {
        super(label, sessionFile, mainFrame);
        this.mainFrame = mainFrame;
        this.sessionFile = sessionFile;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (sessionFile == null || autoload == false)
        {
            File lastSessionDirectory = PreferenceManager.getInstance().getLastSessionDirectory();
            File tmpFile = FileDialogUtils.chooseFile("Open Session", lastSessionDirectory, JFileChooser.FILES_ONLY);

            if (tmpFile == null)
            {
                return;
            }
            sessionFile = tmpFile.getAbsolutePath();
        }
        restoreSession();
    }

    public void restoreSession()
    {

        // If anything has been loaded warn the users. Popping up the
        // warning all the time will get annoying.
        if (IGV.getInstance().getAllTracks().size() > 0)
        {
            int status = JOptionPane.showConfirmDialog(mainFrame.getMainFrame(), UIConstants.OVERWRITE_SESSION_MESSAGE,
                    null, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

            if (status == JOptionPane.CANCEL_OPTION || status == JOptionPane.CLOSED_OPTION)
            {
                return;
            }
        }

        if (sessionFile != null)
        {
            if (FileUtils.isRemote(sessionFile))
            {
                boolean merge = false;
                mainFrame.doRestoreSession(sessionFile, null, merge);
            }
            else
            {
                File f = new File(sessionFile);
                mainFrame.doRestoreSession(f, null);
            }
        }
    }
}
