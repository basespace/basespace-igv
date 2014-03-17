package com.illumina.basespace.igv.session;


import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.broad.igv.PreferenceManager;
import org.broad.igv.session.Session;
import org.broad.igv.session.SessionWriter;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.UIConstants;
import org.broad.igv.ui.WaitCursorManager;
import org.broad.igv.ui.action.SaveSessionMenuAction;
import org.broad.igv.ui.util.FileDialogUtils;

import com.illumina.basespace.igv.BaseSpaceMain;

public class BaseSpaceSaveSessionMenuAction extends SaveSessionMenuAction
{
    public IGV mainFrame;

    public BaseSpaceSaveSessionMenuAction(String label, int mnemonic, IGV mainFrame)
    {
        super(label, mnemonic, mainFrame);
        this.mainFrame = mainFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

        File sessionFile = null;

        String currentSessionFilePath = mainFrame.getSession().getPath();

        String initFile = currentSessionFilePath == null ? UIConstants.DEFAULT_SESSION_FILE : currentSessionFilePath;
        sessionFile = FileDialogUtils.chooseFile("Save Session",
                PreferenceManager.getInstance().getLastSessionDirectory(),
                new File(initFile),
                FileDialogUtils.SAVE);


        if (sessionFile == null) {
            mainFrame.resetStatusMessage();
            return;
        }


        String filePath = sessionFile.getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".xml")) {
            sessionFile = new File(filePath + ".xml");
        }

        mainFrame.setStatusBarMessage("Saving session to " + sessionFile.getAbsolutePath());


        final File sf = sessionFile;
        WaitCursorManager.CursorToken token = WaitCursorManager.showWaitCursor();
        try {

            Session currentSession = mainFrame.getSession();
            currentSession.setPath(sf.getAbsolutePath());
            (new SessionWriter()).saveSession(currentSession, sf);

            // No errors so save last location
            PreferenceManager.getInstance().setLastSessionDirectory(sf.getParentFile());

        } catch (Exception e2) {
            JOptionPane.showMessageDialog(mainFrame.getMainFrame(), "There was an error writing to " + sf.getName() + "(" + e2.getMessage() + ")");
            BaseSpaceMain.logger.log(Level.SEVERE,"Failed to save session!", e2);
        } finally {
            WaitCursorManager.removeWaitCursor(token);
            mainFrame.resetStatusMessage();


        }
    }
    
    

}
