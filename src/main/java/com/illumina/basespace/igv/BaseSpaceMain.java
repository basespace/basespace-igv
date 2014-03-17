package com.illumina.basespace.igv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.broad.igv.ui.IGV;
import org.broad.igv.ui.Main;
import org.broad.igv.ui.UIConstants;
import org.broad.igv.ui.action.MenuAction;
import org.broad.igv.ui.event.ViewChange;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.util.StringUtils;

import com.google.common.eventbus.Subscribe;
import com.illumina.basespace.ApiClient;
import com.illumina.basespace.ApiClientManager;
import com.illumina.basespace.ApiConfiguration;
import com.illumina.basespace.entity.AppResultCompact;
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.igv.session.BaseSpaceOpenSessionMenuAction;
import com.illumina.basespace.igv.session.BaseSpaceSaveSessionMenuAction;
import com.illumina.basespace.igv.ui.BaseSpaceHelper;
import com.illumina.basespace.igv.ui.tree.BrowserDialog;
import com.illumina.basespace.igv.ui.tree.UserNode;
import com.illumina.basespace.igv.vcf.BaseSpaceVariantSetApiFeatureReader;
import com.illumina.basespace.infrastructure.BaseSpaceException;
import com.illumina.basespace.param.FileParams;
import com.illumina.basespace.response.ListFilesResponse;
import com.illumina.basespace.util.BrowserLaunch;

/**
 * 
 * @author bking
 * 
 */
public class BaseSpaceMain extends Main implements SingleInstanceListener
{
    private SingleInstanceService singleInstanceServer;
    public static Logger logger = Logger.getLogger(BaseSpaceMain.class.getPackage().getName());
    private Map<UUID, ClientContext> clients = new Hashtable<UUID, ClientContext>();
    private static BaseSpaceMain instance;
    private final static Object lock = new Object();
    private JMenu baseSpaceMenu;
    private JCheckBoxMenuItem logApiMenu;
    private static Logger connectionLogger;

    static
    {
        connectionLogger = Logger.getLogger(ApiClient.class.getPackage().getName());
        ConsoleHandler consoleHandler = new ConsoleHandler();
        connectionLogger.addHandler(consoleHandler);
        connectionLogger.setUseParentHandlers(false);
    }

    private BaseSpaceMain()
    {
        try
        {
            singleInstanceServer = (SingleInstanceService) ServiceManager.lookup("javax.jnlp.SingleInstanceService");
            singleInstanceServer.addSingleInstanceListener(this);
        }
        catch (UnavailableServiceException e)
        {
            logger.warning("Single instance service not available");
        }

        baseSpaceMenu = new JMenu("BaseSpace");
        IGV.getRootPane().getJMenuBar().add(baseSpaceMenu, IGV.getRootPane().getJMenuBar().getMenuCount() - 1);

        JMenuItem mnuDocumentation = new JMenuItem("BaseSpace IGV Documentation");
        mnuDocumentation.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                BrowserLaunch.openURL("https://github.com/basespace/basespace-igv/wiki/BaseSpace-IGV-Documentation");
            }
        });
        baseSpaceMenu.add(mnuDocumentation);

        JMenuItem mnuBrowser = new JMenuItem("Launch Browser");
        mnuBrowser.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                showBrowser();
            }
        });
        baseSpaceMenu.add(mnuBrowser);

        logApiMenu = new JCheckBoxMenuItem("Debug Mode");
        logApiMenu.setSelected(false);
        logApiMenu.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setDebug(logApiMenu.isSelected());
            }
        });
        baseSpaceMenu.add(logApiMenu);

        baseSpaceMenu.addSeparator();
        JMenuItem miAbout = new JMenuItem("About...");
        miAbout.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                AboutDialog dlgAbout = new AboutDialog();
                dlgAbout.setLocationRelativeTo(IGV.getMainFrame());
                dlgAbout.setVisible(true);

            }
        });
        baseSpaceMenu.add(miAbout);

    }


    protected void setDebug(boolean debug)
    {
        Level level = debug ? Level.ALL : Level.INFO;
        for (Handler handler : connectionLogger.getHandlers())
        {
            handler.setLevel(level);
        }
        connectionLogger.setLevel(level);
    }

    public static BaseSpaceMain instance()
    {
        if (instance != null) return instance;
        synchronized (lock)
        {
            instance = new BaseSpaceMain();
        }
        return instance;
    }

    public static void main(String[] args)
    {
        String[] baseSpaceArgs = BaseSpaceConfiguration.extractBaseSpaceArgs(args);
        String[] baseSpaceArgsWithGenome = new String[baseSpaceArgs.length + 2];
        System.arraycopy(baseSpaceArgs, 0, baseSpaceArgsWithGenome, 0, baseSpaceArgs.length);
        baseSpaceArgsWithGenome[baseSpaceArgs.length] = "-g";
        baseSpaceArgsWithGenome[baseSpaceArgs.length + 1] = "hg19";
        Main.main(BaseSpaceConfiguration.removeBaseSpaceArgs(baseSpaceArgsWithGenome));
        IGV.getInstance().setTrackLoader(new BaseSpaceTrackLoader());
        instance().createBaseSpaceClient(baseSpaceArgs);
    }

    @Override
    public void newActivation(String[] args)
    {
        logger.info("Handling new webstart activation");
        createBaseSpaceClient(BaseSpaceConfiguration.extractBaseSpaceArgs(args));
    }

    public ApiClient getApiClient(UUID id)
    {
        return clients.get(id).getApiClient();
    }

    /**
     * Do not show access token in log. 
     * @param args
     */
    public void createBaseSpaceClient(String[] args)
    {
        if (args == null || args.length == 0)
        {
            throw new Error("No startup arguments Provided to IGV");
        }

        logger.info("Create BaseSpace Client with args:");
        int index = 0;
        for (String arg : args)
        {
        	if(arg.indexOf("token") < 0)
        		logger.info("\t[" + (++index) + "]=" + arg);
        }

        final BaseSpaceConfiguration config = BaseSpaceConfiguration.create(args);
        ApiConfiguration apiConfig = config.getApiConfiguration();
        final ApiClient client = ApiClientManager.instance().createClient(apiConfig);
        final UUID id = UUID.randomUUID();
        clients.put(id, new ClientContext(client, config));

        String appResultId = config.getProperty(BaseSpaceConfiguration.APPRESULT_ID);
        if (appResultId != null)
        {
            AppResultCompact appResult = new AppResultCompact(appResultId);
            FileParams fileParams = new FileParams(new String[] { ".bam", ".bai", ".vcf" }, 10);
            ListFilesResponse resp = client.getFiles(appResult, fileParams);
            BaseSpaceTrackLoader.loadTracks(id, resp);
        }

        // Load passed-in file ids
        String fileIdsValue = config.getProperty(BaseSpaceConfiguration.FILE_ID);
        if (fileIdsValue != null)
        {
            List<FileCompact> files = new ArrayList<FileCompact>();
            String[] fileIds = fileIdsValue.split(",");
            for (String fileId : fileIds)
            {
                try
                {
                    files.add(client.getFile(fileId).get());
                }
                catch (Throwable t)
                {
                    BaseSpaceHelper.showErrorDialog(IGV.getMainFrame(), t);
                }
            }
            BaseSpaceTrackLoader.loadTracks(id, files);
        }

        String debug = config.getProperty(BaseSpaceConfiguration.DEBUG);
        if (debug != null)
        {
            logApiMenu.setSelected(Boolean.parseBoolean(debug));
            setDebug(Boolean.parseBoolean(debug));
        }
        launchBrowser(id);
    }

    protected void launchBrowser(final UUID id)
    {
        try
        {
            ApiClient client = clients.get(id).getApiClient();
            UserNode userNode = new UserNode(client.getCurrentUser().get(), id, clients.get(id));
            BrowserDialog.instance().getBrowserPanel().addUserNode(userNode);
        }
        catch (BaseSpaceException bse)
        {
            BaseSpaceHelper.showErrorDialog(IGV.getMainFrame(), bse);
        }
        showBrowser();
    }

    protected void showBrowser()
    {
        BrowserDialog dialog = BrowserDialog.instance();
        dialog.setLocationRelativeTo(IGV.getMainFrame());
        dialog.setVisible(true);
        dialog.toFront();
    }

    public JMenu getBaseSpaceMenu()
    {
        return baseSpaceMenu;
    }

    public class ClientContext
    {
        private ApiClient client;
        private BaseSpaceConfiguration config;
        private long createTime;

        public ClientContext(ApiClient client, BaseSpaceConfiguration config)
        {
            super();
            this.client = client;
            this.config = config;
            this.createTime = System.currentTimeMillis();
        }

        public ApiClient getApiClient()
        {
            return client;
        }

        public BaseSpaceConfiguration getConfig()
        {
            return config;
        }

        public long getCreateTime()
        {
            return createTime;
        }

    }
}
