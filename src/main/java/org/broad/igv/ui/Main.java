/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL), 
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.ui;

import jargs.gnu.CmdLineParser;

import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.apache.log4j.Logger;
import org.broad.igv.DirectoryManager;
import org.broad.igv.Globals;
import org.broad.igv.PreferenceManager;
import org.broad.igv.ui.event.GlobalKeyDispatcher;
import org.broad.igv.util.HttpUtils;
import org.broad.igv.util.StringUtils;

import com.illumina.basespace.BaseSpaceConfiguration;
import com.illumina.basespace.BaseSpaceSession;
import com.illumina.basespace.BaseSpaceSessionManager;
import com.illumina.desktop.DockingFrame;
import com.illumina.desktop.lnf.IlluminaLookAndFeel;
import com.illumina.desktop.lnf.IlluminaTheme;
import com.illumina.igv.IlluminaIGVProperties;
import com.jidesoft.plaf.LookAndFeelFactory;


/**
 * Utility class for launching IGV.  Provides a "main" method and an "open"  method for opening IGV in a supplied Frame.
 * <p/>
 * Note: The "open" methods must be executed on the event thread, for example
 * <p/>
 * public static void main(String[] args) {
 * EventQueue.invokeLater(new Runnable() {
 * public void run() {
 * Frame frame = new Frame();
 * org.broad.igv.ui.Main.open(frame);
 * }
 * );
 * }
 *
 * @author jrobinso
 * @date Feb 7, 2011
 */
public class Main {

    private static Logger log = Logger.getLogger(Main.class);

    /**
     * Launch an igv instance as a stand-alone application in its own Frame.
     *
     * @param args
     */
    public static void main(final String args[]) {

        Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());

        initApplication();

        DockingFrame frame = new DockingFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        open(frame, args);

    }

    private static void initApplication() {
        DirectoryManager.initializeLog();
        log.info("Startup  " + Globals.applicationString());
        log.info("Default User Directory: " + DirectoryManager.getUserDirectory());
        System.setProperty("http.agent", Globals.applicationString());

        Runtime.getRuntime().addShutdownHook(new ShutdownThread());

        // TODO -- get these from user preferences
        ToolTipManager.sharedInstance().setEnabled(true);
        ToolTipManager.sharedInstance().setInitialDelay(50);
        ToolTipManager.sharedInstance().setReshowDelay(50);
        ToolTipManager.sharedInstance().setDismissDelay(60000);


        // Anti alias settings.   TODO = Are these neccessary anymore ?
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");


    }


    /**
     * Open an IGV instance in the supplied Frame.
     *
     * @param frame
     */
    public static void open(DockingFrame frame) {

        open(frame, new String[]{});
    }


    /**
     * Open an IGV instance in the supplied frame.
     *
     * @param frame
     * @param args  command-line arguments
     */
    public static void open(DockingFrame frame, String[] args) {

        // Add a listener for the "close" icon, unless its a JFrame
        if (!(frame instanceof JFrame)) {
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent windowEvent) {
                    windowEvent.getComponent().setVisible(false);
                }
            });
        }

        // Turn on tooltip in case it was disabled for a temporary keyboard event, e.g. alt-tab
        frame.addWindowListener(new WindowAdapter() {

            public void windowActivated(WindowEvent e) {
                if (IGV.hasInstance() && !IGV.getInstance().isSuppressTooltip()) {
                    ToolTipManager.sharedInstance().setEnabled(true);
                }
            }

            @Override
            public void windowGainedFocus(WindowEvent windowEvent) {
                if (IGV.hasInstance() && !IGV.getInstance().isSuppressTooltip()) {
                    ToolTipManager.sharedInstance().setEnabled(true);
                }
            }
        });

        initializeLookAndFeel();

        Main.IGVArgs igvArgs = new Main.IGVArgs(args);

        // Optional arguments
        if (igvArgs.getPropertyOverrides() != null) {
            PreferenceManager.getInstance().loadOverrides(igvArgs.getPropertyOverrides());
        }
        if (igvArgs.getDataServerURL() != null) {
            PreferenceManager.getInstance().overrideDataServerURL(igvArgs.getDataServerURL());
        }
        if (igvArgs.getGenomeServerURL() != null) {
            PreferenceManager.getInstance().overrideGenomeServerURL(igvArgs.getGenomeServerURL());
        }

        HttpUtils.getInstance().updateProxySettings();
        bootstrapBaseSpace(frame, igvArgs); 
        
        //IGV.createInstance(frame).startUp(igvArgs);


    }

    
    private static void bootstrapBaseSpace(final DockingFrame frame,final Main.IGVArgs igvArgs)
    {
        try
        {
            BaseSpaceConfiguration config = IlluminaIGVProperties.instance().createConfig();
            log.info(IlluminaIGVProperties.instance().toString());
            BaseSpaceSession session = BaseSpaceSessionManager.instance().requestSession(config);    
            IGV.createInstance(frame,session).startUp(igvArgs);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new GlobalKeyDispatcher());
        }
        catch(Throwable t)
        {
            t.printStackTrace();
            JOptionPane.showMessageDialog(frame, t.getMessage());
            System.exit(0);
        }
    }        
        
    
    private static void initializeLookAndFeel() {

        try {
            
            //String lnf = UIManager.getSystemLookAndFeelClassName();
            //UIManager.setLookAndFeel(lnf);
        
            IlluminaTheme nt = new IlluminaTheme();
            IlluminaLookAndFeel landf = new IlluminaLookAndFeel();
            IlluminaLookAndFeel.setCurrentTheme( nt);
            UIManager.setLookAndFeel( landf);
        
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Globals.IS_LINUX) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                UIManager.put("JideSplitPane.dividerSize", 5);
                UIManager.put("JideSplitPaneDivider.background", Color.darkGray);

            } catch (Exception exception) {
                exception.printStackTrace();
            }

        }
        // Todo -- what does this do?
        LookAndFeelFactory.installJideExtension();
    }

    /**
     * Class to encapsulate IGV command line arguments.
     */
    static public  class IGVArgs {
        private String batchFile = null;
        private String sessionFile = null;
        private String dataFileString = null;
        private String locusString = null;
        private String propertyOverrides = null;
        private String genomeId = null;
        private String port = null;
        private String dataServerURL = null;
        private String genomeServerURL = null;
        private String indexFile = null;
        private String name = null;

        IGVArgs(String[] args) {
            if (args != null) {
                parseArgs(args);
            }
        }

        /**
         * Parse arguments.  All arguments are optional,  a full set of arguments are
         * firstArg  locusString  -b batchFile -p preferences
         */
        private void parseArgs(String[] args) {
            CmdLineParser parser = new CmdLineParser();
            CmdLineParser.Option propertyFileOption = parser.addStringOption('o', "preferences");
            CmdLineParser.Option batchFileOption = parser.addStringOption('b', "batch");
            CmdLineParser.Option portOption = parser.addStringOption('p', "port");
            CmdLineParser.Option genomeOption = parser.addStringOption('g', "genome");
            CmdLineParser.Option dataServerOption = parser.addStringOption('d', "dataServerURL");
            CmdLineParser.Option genomeServerOption = parser.addStringOption('u', "genomeServerURL");
            CmdLineParser.Option indexFileOption = parser.addStringOption('i', "indexFileURL");
            CmdLineParser.Option nameOption = parser.addStringOption('n', "name");

            try {
                parser.parse(args);
            } catch (CmdLineParser.IllegalOptionValueException e) {
                e.printStackTrace();  // This is not logged because the logger is not initialized yet.
            } catch (CmdLineParser.UnknownOptionException e) {
                e.printStackTrace();
            }
            propertyOverrides = (String) parser.getOptionValue(propertyFileOption);
            batchFile = (String) parser.getOptionValue(batchFileOption);
            port = (String) parser.getOptionValue(portOption);
            genomeId = (String) parser.getOptionValue(genomeOption);
            dataServerURL = (String) parser.getOptionValue(dataServerOption);
            genomeServerURL = (String) parser.getOptionValue(genomeServerOption);
            indexFile = (String) parser.getOptionValue(indexFileOption);
            name = (String) parser.getOptionValue(nameOption);

            String[] nonOptionArgs = parser.getRemainingArgs();
            if (nonOptionArgs != null && nonOptionArgs.length > 0) {
                //String firstArg = StringUtils.decodeURL(nonOptionArgs[0]);
                String firstArg = StringUtils.decodeURL(nonOptionArgs[0]);
                if (firstArg != null && !firstArg.equals("ignore")) {
                    log.info("Loading: " + firstArg);
                    if (firstArg.endsWith(".xml") || firstArg.endsWith(".php") || firstArg.endsWith(".php3")
                            || firstArg.endsWith(".session")) {
                        sessionFile = firstArg;
                    } else {
                        dataFileString = firstArg;
                    }
                }
                if (nonOptionArgs.length > 1) {
                    locusString = nonOptionArgs[1];
                }
            }
        }

        public String getBatchFile() {
            return batchFile;
        }

        public String getSessionFile() {
            return sessionFile;
        }

        public String getDataFileString() {
            return dataFileString;
        }

        public String getLocusString() {
            return locusString;
        }

        public String getPropertyOverrides() {
            return propertyOverrides;
        }

        public String getGenomeId() {
            return genomeId;
        }

        public String getPort() {
            return port;
        }

        public String getDataServerURL() {
            return dataServerURL;
        }

        public String getGenomeServerURL() {
            return genomeServerURL;
        }

        public String getIndexFile() {
            return indexFile;
        }

        public String getName() {
            return name;
        }
    }

}
