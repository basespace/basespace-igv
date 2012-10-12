/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTIES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */
/*
 * IGVCommandBar.java
 *
 * Created on April 5, 2008, 10:02 AM
 */
package org.broad.igv.ui;


import com.illumina.desktop.ImageProvider;
import com.jidesoft.hints.ListDataIntelliHints;
import com.jidesoft.swing.JideBoxLayout;
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;
import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.PreferenceManager;
import org.broad.igv.dev.affective.AffectiveUtils;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.feature.Cytoband;
import org.broad.igv.feature.FeatureDB;
import org.broad.igv.feature.NamedFeature;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeListItem;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.feature.genome.GenomeServerException;
import org.broad.igv.session.History;
import org.broad.igv.ui.action.FitDataToWindowMenuAction;
import org.broad.igv.ui.action.SearchCommand;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.ui.panel.ZoomSliderPanel;
import org.broad.igv.ui.util.IconFactory;
import org.broad.igv.ui.util.ProgressBar;
import org.broad.igv.ui.util.ProgressMonitor;
import org.broad.igv.ui.util.UIUtilities;
import org.broad.igv.util.LongRunningTask;
import org.broad.igv.util.NamedRunnable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.util.*;
import java.util.List;

/**
 * @author jrobinso
 */
public class IGVCommandBar extends javax.swing.JPanel {

    private static Logger log = Logger.getLogger(IGVCommandBar.class);

    final static String DISABLE_POPUP_TOOLTIP = "Disable popup text in data panels.";
    final static String ENABLE_POPUP_TOOLTIP = "Enable popup text in data panels.";


    // TODO -- THESE LISTS ARE ALSO DEFINED IN GENOME MANAGER  ???
    private List<GenomeListItem> userDefinedGenomeItemList;
    private List<GenomeListItem> serverGenomeItemList;
    private List<GenomeListItem> cachedGenomeItemList;

    private JComboBox chromosomeComboBox;
    private JComboBox genomeComboBox;
    //private JPanel geneListPanel;
    // private JideButton geneListLabel;
    private JideButton goButton;
    private JideButton homeButton;
    private JPanel locationPanel;
    private JideButton refreshButton;
    private JideToggleButton roiToggleButton;
    private JideButton supressTooltipButton;
    private JTextField searchTextField;
    private JPanel toolPanel;
    private JPanel zoomControl;
    final private int DEFAULT_CHROMOSOME_DROPDOWN_WIDTH = 120;
    private JideButton backButton;
    private JideButton forwardButton;
    private JideButton fitToWindowButton;
    private boolean suppressTooltip = false;

    /**
     * Creates new form IGVCommandBar
     */
    public IGVCommandBar() {
        initComponents();

        // Initialize controls
        SearchHints hints = new SearchHints(this.searchTextField);

        String currentChr = getDefaultReferenceFrame().getChrName();
        boolean isWholeGenome = currentChr.equals(Globals.CHR_ALL);

        chromosomeComboBox.setSelectedItem(currentChr);
        roiToggleButton.setEnabled(!isWholeGenome);
        zoomControl.setEnabled(!isWholeGenome);
    }

    /**
     * Method description
     *
     * @param genome
     * @return
     */
    public boolean isGenomeCached(String genome) {
        boolean isCached = false;

        if ((cachedGenomeItemList != null) && !cachedGenomeItemList.isEmpty()) {
            for (GenomeListItem item : cachedGenomeItemList) {
                if (item.getId().equalsIgnoreCase(genome)) {
                    isCached = true;
                }
            }
        }
        return isCached;
    }

    /**
     * This method is called once on startup
     *
     * @param monitor
     * @throws FileNotFoundException
     * @throws NoRouteToHostException
     */
    public void initializeGenomeList(final ProgressMonitor monitor)
            throws FileNotFoundException, NoRouteToHostException {

        if (log.isDebugEnabled()) {
            log.debug("Enter initializeGenomeList");
        }

        if (monitor != null) {
            monitor.fireProgressChange(1);
        }

        genomeComboBox.removeAllItems();
        genomeComboBox.setRenderer(new ComboBoxRenderer());
        genomeComboBox.setToolTipText(UIConstants.CHANGE_GENOME_TOOLTIP);
        rebuildGenomeItemList(null);

        if (monitor != null) {
            monitor.fireProgressChange(50);
        }

        genomeComboBox.addActionListener(new GenomeBoxActionListener());

        // Post creation widget setup.
        searchTextField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionevent) {
                goButtonActionPerformed(actionevent);
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("Exit initializeGenomeList");
        }

    }

    class GenomeBoxActionListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {

            final Runnable runnable = new Runnable() {

                public void run() {
                    GenomeListItem genomeListItem = (GenomeListItem) genomeComboBox.getSelectedItem();
                    if (genomeListItem != null) {
                        final IGV igv = IGV.getInstance();
                        final ProgressMonitor monitor = new ProgressMonitor();
                        final ProgressBar bar =
                                ProgressBar.showProgressDialog(IGV.getMainFrame(), "Loading Genome...", monitor, false);


                        try {
                            monitor.fireProgressChange(50);

                            Genome genome;

                            if (genomeListItem == AffectiveUtils.GENOME_DESCRIPTOR) {
                                genome = AffectiveUtils.getGenome();
                                igv.getGenomeManager().setCurrentGenome(genome);
                            } else {
                                //If this is the same as currently loaded genome, no need to
                                //do anything. Mainly to prevent double calling
                                if (genomeListItem.getId().equalsIgnoreCase(igv.getGenomeManager().getGenomeId())) {
                                    genome = igv.getGenomeManager().getCurrentGenome();
                                } else {
                                    genome = igv.getGenomeManager().loadGenome(genomeListItem.getLocation(), null);

                                }
                            }

                            updateGenome(genome);
                            monitor.fireProgressChange(25);

                            if (!isGenomeCached(genomeListItem.getId())) {
                                cachedGenomeItemList.add(genomeListItem);
                            }

                            // TODO -- warn user.
                            igv.resetSession(null);

                            PreferenceManager.getInstance().setDefaultGenome(genomeListItem.getId());
                            monitor.fireProgressChange(25);

                            IGV.getInstance().doRefresh();

                        } catch (GenomeServerException e) {
                            log.error("Error loading genome: " + genomeListItem.getLocation(), e);
                            JOptionPane.showMessageDialog(
                                    IGV.getMainFrame(),
                                    "Error loading genome: " + genomeListItem.getDisplayableName());
                        } catch (IOException e) {
                            if (bar != null) {
                                bar.close();
                            }

                            int choice =
                                    JOptionPane.showConfirmDialog(
                                            IGV.getMainFrame(), "The genome file [" + e.getMessage() +
                                            "] could not be read. Would you like to remove the selected entry?",
                                            "", JOptionPane.OK_CANCEL_OPTION);

                            if (choice == JOptionPane.OK_OPTION) {
                                Set<String> excludedArchivesUrls = new HashSet();
                                excludedArchivesUrls.add(genomeListItem.getLocation());
                                rebuildGenomeItemList(excludedArchivesUrls);
                            }
                        } catch (Exception e) {
                            log.error("Error initializing genome");
                        } finally {
                            if (bar != null) {
                                bar.close();
                            }
                        }

                    }
                }
            };

            // If we're on the dispatch thread spawn a worker, otherwise just execute.
            LongRunningTask.submit(runnable);
        }
    }


    /**
     * Adds the new user-defined genome to the drop down list.
     *
     * @param newItem
     */
    public void addToUserDefinedGenomeItemList(GenomeListItem newItem) {


        if (userDefinedGenomeItemList == null) {
            userDefinedGenomeItemList = new LinkedList<GenomeListItem>();
            userDefinedGenomeItemList.add(newItem);
        } else {

            List tempItemList = new LinkedList<GenomeListItem>();
            tempItemList.add(newItem);
            for (GenomeListItem item : userDefinedGenomeItemList) {
                tempItemList.add(item);
            }
            userDefinedGenomeItemList = tempItemList;
        }
        genomeComboBox.setModel(getModelForGenomeListComboBox());

    }

    /**
     * Completely rebuild the genome drop down info from scratch.
     *
     * @param excludedArchivesUrls
     */
    public void rebuildGenomeItemList(Set excludedArchivesUrls) {

        try {
            // Build a single available genome list from both client, server
            // and cached information. This allows us to process
            // everything the same way.
            List<GenomeListItem> serverSideItemList = null;
            List<GenomeListItem> cacheGenomeItemList = null;
            List<GenomeListItem> clientSideItemList = null;

            boolean affectiveMode = PreferenceManager.getInstance().getAsBoolean(PreferenceManager.AFFECTIVE_ENABLE);
            if (affectiveMode) {
                serverSideItemList = Arrays.asList(AffectiveUtils.GENOME_DESCRIPTOR);
            } else {

                final GenomeManager genomeManager = IGV.getInstance().getGenomeManager();
                try {
                    serverSideItemList = genomeManager.getServerGenomeArchiveList(excludedArchivesUrls);
                } catch (Exception e) {

                    UIUtilities.invokeOnEventThread(new Runnable() {

                        public void run() {
                            JOptionPane.showMessageDialog(
                                    IGV.getMainFrame(),
                                    UIConstants.CANNOT_ACCESS_SERVER_GENOME_LIST);
                        }
                    });
                }


                if (serverSideItemList == null || serverSideItemList.isEmpty()) {
                    cacheGenomeItemList = genomeManager.getCachedGenomeArchiveList();
                }

                clientSideItemList = genomeManager.getUserDefinedGenomeArchiveList();
            }

            setGenomeItemList(clientSideItemList, serverSideItemList, cacheGenomeItemList);

            genomeComboBox.setModel(getModelForGenomeListComboBox());

        } catch (Exception e) {
            log.error("Failed to get genome archive list " + "information from the server!", e);
        }
    }


    void updateChromosomeDropdown() {

        final Genome genome = IGV.getInstance().getGenomeManager().getCurrentGenome();
        if (genome == null) return;

        List<String> tmp = new LinkedList(genome.getChromosomeNames());
        if (tmp.size() > 1) {
            String homeChr = genome.getHomeChromosome();
            if (homeChr.equals(Globals.CHR_ALL)) {
                tmp.add(Globals.CHR_ALL);
            }
        }

        Graphics2D graphics2D = (Graphics2D) chromosomeComboBox.getGraphics();
        Font font = chromosomeComboBox.getFont();
        FontMetrics fontMetrics = chromosomeComboBox.getFontMetrics(font);

        int w = DEFAULT_CHROMOSOME_DROPDOWN_WIDTH;
        for (String chromosomeName : tmp) {
            Rectangle2D textBounds = fontMetrics.getStringBounds(chromosomeName, graphics2D);
            if (textBounds != null) {
                int width = textBounds.getBounds().width + 50;

                // int width = chromosomeName.length()*fontSize-(fontSize*4);  // TODO Hack figure out whats's wrong with previous line
                if (width > w) {
                    w = width;
                }
            }
        }

        Object[] chomosomeNames = tmp.toArray();
        final DefaultComboBoxModel defaultModel = new DefaultComboBoxModel(chomosomeNames);
        final int dropdownWidth = w;

        chromosomeComboBox.setModel(defaultModel);
        chromosomeComboBox.setSelectedItem(genome.getHomeChromosome());

        UIUtilities.invokeOnEventThread(new Runnable() {

            public void run() {
                adjustChromosomeDropdownWidth(dropdownWidth);
            }
        });

    }

    protected void chromosomeChanged(String chrName) {
        roiToggleButton.setEnabled(!chrName.equals(Globals.CHR_ALL));
        zoomControl.setEnabled(!chrName.equals(Globals.CHR_ALL));

        if (chromosomeComboBox.getSelectedItem() != null) {
            if (!chromosomeComboBox.getSelectedItem().equals(chrName)) {
                chromosomeComboBox.setSelectedItem(chrName);
            }
        }
    }

    /**
     * Method description
     */
    public void updateCurrentCoordinates() {
        searchTextField.setText("");

        final String chrName = getDefaultReferenceFrame().getChrName();

        if (!chrName.equals(chromosomeComboBox.getSelectedItem())) {
            chromosomeChanged(chrName);
            chromosomeComboBox.setSelectedItem(chrName);
            IGV.getInstance().chromosomeChangeEvent(chrName, false);
        }

        String p = "";

        if (!chrName.equals(Globals.CHR_ALL)) {
            p = getDefaultReferenceFrame().getFormattedLocusString();
        }
        final String position = p;
        UIUtilities.invokeOnEventThread(new Runnable() {

            public void run() {
                searchTextField.setText(position);
            }
        });

        final History history = IGV.getInstance().getSession().getHistory();
        forwardButton.setEnabled(history.canGoForward());
        backButton.setEnabled(history.canGoBack());


    }

    private ReferenceFrame getDefaultReferenceFrame() {
        return FrameManager.getDefaultFrame();
    }

    public void setGeneListMode(boolean geneListMode) {

        genomeComboBox.setEnabled(!geneListMode);
//        locationPanel.setEnabled(!geneListMode);
        chromosomeComboBox.setEnabled(!geneListMode);
//        searchTextField.setEnabled(!geneListMode);
//        goButton.setEnabled(!geneListMode);
        zoomControl.setEnabled(!geneListMode);
//        homeButton.setEnabled(true);
//        roiToggleButton.setEnabled(!geneListMode);
    }


    public boolean isSuppressTooltip() {
        return suppressTooltip;
    }

    static class ComboBoxRenderer implements ListCellRenderer {

        JSeparator separator;

        /**
         * Constructs ...
         */
        public ComboBoxRenderer() {
            separator = new JSeparator(JSeparator.HORIZONTAL);
        }

        /**
         * Method description
         *
         * @param list
         * @param value
         * @param index
         * @param isSelected
         * @param cellHasFocus
         * @return
         */
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String text = (value == null) ? "" : value.toString();

            Component renderer = null;

            if (UIConstants.GENOME_LIST_SEPARATOR.equals(text)) {
                return separator;
            }

            if (text.equals(UIConstants.REMOVE_GENOME_LIST_MENU_ITEM)) {
                JLabel label = new JLabel(text);

                label.setOpaque(true);
                label.setBorder(new EmptyBorder(1, 1, 1, 1));
                renderer = label;
            } else {

                JLabel label = new JLabel(text);

                label.setOpaque(true);
                label.setBorder(new EmptyBorder(1, 1, 1, 1));
                renderer = label;
            }

            if (isSelected) {
                renderer.setBackground(list.getSelectionBackground());
                renderer.setForeground(list.getSelectionForeground());
            } else {
                renderer.setBackground(list.getBackground());
                renderer.setForeground(list.getForeground());
            }

            renderer.setFont(list.getFont());

            return renderer;
        }
    }


    /**
     * Gets the collection of genome display names currently in use.
     *
     * @return Set of display names.
     */
    public Collection<String> getGenomeDisplayNames() {

        Set<String> displayNames = new HashSet();
        int itemCount = genomeComboBox.getItemCount();

        for (int i = 0; i < itemCount; i++) {
            Object object = genomeComboBox.getItemAt(i);
            if (object instanceof GenomeListItem) {
                GenomeListItem genomeListItem = (GenomeListItem) object;
                displayNames.add(genomeListItem.getDisplayableName());
            }
        }
        return displayNames;
    }

    /**
     * Gets the collection of genome list items ids currently in use.
     *
     * @return Set of ids.
     */
    public Collection<String> getGenomeIds() {

        Set<String> ids = new HashSet();
        int itemCount = genomeComboBox.getItemCount();

        for (int i = 0; i < itemCount; i++) {
            Object object = genomeComboBox.getItemAt(i);
            if (object instanceof GenomeListItem) {
                GenomeListItem genomeListItem = (GenomeListItem) object;
                ids.add(genomeListItem.getId());
            }
        }
        return ids;
    }

    /**
     * Selects the first genome from the list,
     *
     * @param genomeId
     */
    public void selectGenomeFromListWithNoImport(String genomeId) {

        int itemCount = genomeComboBox.getItemCount();

        for (int i = 0; i < itemCount; i++) {
            Object object = genomeComboBox.getItemAt(i);

            if (object instanceof GenomeListItem) {
                GenomeListItem genomeListItem = (GenomeListItem) object;

                // If the list genome matchs the one we are interested in
                // process it
                String id = genomeListItem.getId();
                if ((id != null) && id.trim().equalsIgnoreCase(genomeId)) {
                    genomeComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Called from session loading,  command line listener, startup code
     */
    public void selectGenomeFromList(final String genomeId) {


        // See if this genome is already loaded
        String currentGenomeId = IGV.getInstance().getGenomeManager().getGenomeId();
        if (currentGenomeId != null && genomeId != null && genomeId.equalsIgnoreCase(currentGenomeId)) {
            return;
        }


        log.debug("Run selectGenomeFromList");


        boolean wasFound = false;

        // Now select this item in tne comboBox

        if (genomeId != null) {

            int itemCount = genomeComboBox.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                Object object = genomeComboBox.getItemAt(i);

                if (object instanceof GenomeListItem) {

                    GenomeListItem genomeListItem = (GenomeListItem) object;
                    String id = genomeListItem.getId();

                    // If the list genome matchs the one we are interested in
                    // process it
                    if ((id != null) && id.trim().equalsIgnoreCase(genomeId)) {

                        genomeComboBox.setSelectedIndex(i);
                        wasFound = true;


                        break;
                    }
                }
            }

        }


        // If genome archive was not found use first item
        // we have in the list
        if (!wasFound) {
            int count = genomeComboBox.getItemCount();

            for (int i = 0; i < count; i++) {
                Object object = genomeComboBox.getItemAt(i);

                if (object instanceof GenomeListItem) {
                    GenomeListItem item = (GenomeListItem) object;

                    // We found the genome we want moved it to the local cache
                    // if it's not there already
                    genomeComboBox.setSelectedIndex(i);
                    break;

                }
            }
        }
        log.debug("Finish selectGenomeFromList");


    }

    public void updateGenome(Genome genome) {


        FrameManager.getDefaultFrame().invalidateLocationScale();

        for (Chromosome chr : genome.getChromosomes()) {
            final List<Cytoband> cytobands = chr.getCytobands();
            if (cytobands != null) {
                for (Cytoband cyto : cytobands) {
                    FeatureDB.addFeature(cyto.getLongName(), cyto);
                }
            }
        }
        updateChromosomeDropdown();

    }

    /**
     * Build a model for the genome combo box
     *
     * @return
     */
    public DefaultComboBoxModel getModelForGenomeListComboBox() {

        LinkedHashSet<Object> list = new LinkedHashSet();

        if ((userDefinedGenomeItemList != null) && !userDefinedGenomeItemList.isEmpty()) {
            for (GenomeListItem item : userDefinedGenomeItemList) {
                list.add(item);
            }
            list.add(UIConstants.GENOME_LIST_SEPARATOR);
        }

        if ((serverGenomeItemList != null) && !serverGenomeItemList.isEmpty()) {
            for (GenomeListItem item : this.serverGenomeItemList) {
                list.add(item);
            }

            if ((cachedGenomeItemList == null) || cachedGenomeItemList.isEmpty()) {
                list.add(UIConstants.GENOME_LIST_SEPARATOR);
            }
        }

        if ((cachedGenomeItemList != null) && !cachedGenomeItemList.isEmpty()) {
            for (GenomeListItem item : this.cachedGenomeItemList) {
                list.add(item);
            }

            list.add(UIConstants.GENOME_LIST_SEPARATOR);
        }

        return new DefaultComboBoxModel(list.toArray());
    }


    public void setGenomeItemList(List<GenomeListItem> clientItemList,
                                  List<GenomeListItem> serverItemList,
                                  List<GenomeListItem> cachedGenomeItemList) {

        if (clientItemList == null) {
            clientItemList = new LinkedList<GenomeListItem>();
        }

        if (serverItemList == null) {
            serverItemList = new LinkedList<GenomeListItem>();
        }

        if (cachedGenomeItemList == null) {
            cachedGenomeItemList = new LinkedList<GenomeListItem>();
        }


        this.userDefinedGenomeItemList = clientItemList;
        this.cachedGenomeItemList = cachedGenomeItemList;
        this.serverGenomeItemList = serverItemList;
    }


    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {

        setMinimumSize(new Dimension(200, 32));

        // setPreferredSize(new Dimension(800, 32));

        JideBoxLayout layout = new JideBoxLayout(this, JideBoxLayout.X_AXIS);

        setLayout(layout);

        // This controls the vertical height of the command bar

        locationPanel = new javax.swing.JPanel();
        locationPanel.setBorder(new LineBorder(Color.lightGray, 1, true));

        // BorderFactory.createMatteBorder(2, 2, 2, 2, Color.lightGray));
        // new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        locationPanel.setPreferredSize(new java.awt.Dimension(150, 20));
        locationPanel.setLayout(new JideBoxLayout(locationPanel, JideBoxLayout.X_AXIS));
        locationPanel.setAlignmentY(CENTER_ALIGNMENT);
        locationPanel.add(Box.createRigidArea(new Dimension(10, 36)), JideBoxLayout.FIX);

        genomeComboBox = new JComboBox();
        genomeComboBox.setMinimumSize(new Dimension(180, 27));
        genomeComboBox.setPreferredSize(new Dimension(180, 27));
        locationPanel.add(genomeComboBox, JideBoxLayout.FIX);
        locationPanel.add(Box.createHorizontalStrut(5), JideBoxLayout.FIX);

        chromosomeComboBox = new javax.swing.JComboBox();
        chromosomeComboBox.setToolTipText("Select a chromosome to view");
        chromosomeComboBox.setMaximumSize(new java.awt.Dimension(DEFAULT_CHROMOSOME_DROPDOWN_WIDTH, 30));
        chromosomeComboBox.setMinimumSize(new java.awt.Dimension(DEFAULT_CHROMOSOME_DROPDOWN_WIDTH, 30));
        chromosomeComboBox.setPreferredSize(new java.awt.Dimension(DEFAULT_CHROMOSOME_DROPDOWN_WIDTH, 30));
        chromosomeComboBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chromosomeComboBoxActionPerformed(evt);
            }
        });
        locationPanel.add(chromosomeComboBox, JideBoxLayout.FIX);
        locationPanel.add(Box.createHorizontalStrut(5), JideBoxLayout.FIX);

        searchTextField = new JTextField();
        searchTextField.setToolTipText("Enter a gene of locus, e.f. EGFR,   chr1,   or chr1:100,000-200,000");
        searchTextField.setMaximumSize(new java.awt.Dimension(250, 15));
        searchTextField.setMinimumSize(new java.awt.Dimension(100, 28));
        searchTextField.setPreferredSize(new java.awt.Dimension(230, 28));
        searchTextField.setAlignmentY(CENTER_ALIGNMENT);

        locationPanel.add(searchTextField, JideBoxLayout.FIX);

        goButton = new JideButton("Go");
        // goButton.setButtonStyle(ButtonStyle.TOOLBOX_STYLE);

        // goButton.setPreferredSize(new java.awt.Dimension(30, 30));
        // goButton.setMaximumSize(new java.awt.Dimension(30, 30));
        // goButton.setMinimumSize(new java.awt.Dimension(30, 30));
        // goButton.setText("Go");
        goButton.setToolTipText("Jump to gene or locus");
        goButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goButtonActionPerformed(evt);
            }
        });
        locationPanel.add(goButton, JideBoxLayout.FIX);

        add(locationPanel, JideBoxLayout.FIX);

        add(Box.createHorizontalStrut(10), JideBoxLayout.FIX);

        toolPanel = new javax.swing.JPanel();
        toolPanel.setAlignmentX(RIGHT_ALIGNMENT);
        toolPanel.setLayout(new JideBoxLayout(toolPanel, JideBoxLayout.X_AXIS));
        //final Border toolButtonBorder = BorderFactory.createLineBorder(Color.gray, 1);

        homeButton = new com.jidesoft.swing.JideButton();
        homeButton.setAlignmentX(RIGHT_ALIGNMENT);
        //homeButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
        // homeButton.setBorder(toolButtonBorder);
        //homeButton.setIcon(new javax.swing.ImageIcon(
         //       getClass().getResource("/toolbarButtonGraphics/navigation/Home24.gif")));
        
        homeButton.setIcon(ImageProvider.instance().getIcon("home_nav.gif"));
        homeButton.setMaximumSize(new java.awt.Dimension(32, 32));
        homeButton.setMinimumSize(new java.awt.Dimension(32, 32));
        homeButton.setPreferredSize(new java.awt.Dimension(32, 32));
        homeButton.setToolTipText("Jump to whole genome view");
        homeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homeButtonActionPerformed(evt);
            }
        });
        toolPanel.add(homeButton, JideBoxLayout.FIX);


        // toolPanel.setBorder(
        // new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        backButton = new JideButton();
        //backButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
        //backButton.setBorder(toolButtonBorder);
        backButton.setIcon( ImageProvider.instance().getIcon("back.png"));
        backButton.setToolTipText("Go back");
        backButton.setMaximumSize(new java.awt.Dimension(32, 32));
        backButton.setMinimumSize(new java.awt.Dimension(32, 32));
        backButton.setPreferredSize(new java.awt.Dimension(32, 32));
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IGV.getInstance().getSession().getHistory().back();

            }
        });
        backButton.setEnabled(false);
        toolPanel.add(backButton, JideBoxLayout.FIX);

        forwardButton = new JideButton();
        //forwardButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
        //forwardButton.setBorder(toolButtonBorder);
        forwardButton.setIcon( ImageProvider.instance().getIcon("forward.png"));
        forwardButton.setToolTipText("Go forward");
        forwardButton.setMaximumSize(new java.awt.Dimension(32, 32));
        forwardButton.setMinimumSize(new java.awt.Dimension(32, 32));
        forwardButton.setPreferredSize(new java.awt.Dimension(32, 32));
        forwardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IGV.getInstance().getSession().getHistory().forward();
            }
        });
        forwardButton.setEnabled(false);
        toolPanel.add(forwardButton, JideBoxLayout.FIX);

        refreshButton = new com.jidesoft.swing.JideButton();
        //refreshButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
        //refreshButton.setBorder(toolButtonBorder);
        refreshButton.setAlignmentX(RIGHT_ALIGNMENT);
      //refreshButton.setIcon(new javax.swing.ImageIcon(
            //    getClass().getResource("/toolbarButtonGraphics/general/Refresh24.gif")));    // NOI18N
     
        refreshButton.setIcon( ImageProvider.instance().getIcon("refresh.png"));
        
       
        
        refreshButton.setMaximumSize(new java.awt.Dimension(32, 32));
        refreshButton.setMinimumSize(new java.awt.Dimension(32, 32));
        refreshButton.setPreferredSize(new java.awt.Dimension(32, 32));
        refreshButton.setToolTipText("Refresh the screen");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });
        toolPanel.add(refreshButton, JideBoxLayout.FIX);


        Icon regionOfInterestIcon =
                IconFactory.getInstance().getIcon(IconFactory.IconID.REGION_OF_INTEREST);

        roiToggleButton = new JideToggleButton(regionOfInterestIcon);
        //roiToggleButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
        //roiToggleButton.setBorder(toolButtonBorder);
        roiToggleButton.setAlignmentX(RIGHT_ALIGNMENT);
        roiToggleButton.setToolTipText("Define a region of interest.");
        roiToggleButton.setMaximumSize(new java.awt.Dimension(32, 32));
        roiToggleButton.setMinimumSize(new java.awt.Dimension(32, 32));
        roiToggleButton.setPreferredSize(new java.awt.Dimension(32, 32));
        roiToggleButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                roiToggleButtonActionPerformed(evt);
            }
        });
        toolPanel.add(roiToggleButton, JideBoxLayout.FIX);


        fitToWindowButton = new JideButton();
        //fitToWindowButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
        //fitToWindowButton.setBorder(toolButtonBorder);
        fitToWindowButton.setAlignmentX(RIGHT_ALIGNMENT);
        fitToWindowButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/collapseall.gif")));
        fitToWindowButton.setMaximumSize(new java.awt.Dimension(32, 32));
        fitToWindowButton.setMinimumSize(new java.awt.Dimension(32, 32));
        fitToWindowButton.setPreferredSize(new java.awt.Dimension(32, 32));
        fitToWindowButton.setToolTipText("Resize tracks to fit in window.");
        fitToWindowButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                (new FitDataToWindowMenuAction(null, 0, IGV.getInstance())).actionPerformed(evt);
            }
        });
        toolPanel.add(fitToWindowButton, JideBoxLayout.FIX);

        final Icon noTooltipIcon =
                IconFactory.getInstance().getIcon(IconFactory.IconID.NO_TOOLTIP);
        final Icon tooltipIcon =
                IconFactory.getInstance().getIcon(IconFactory.IconID.TOOLTIP);
        supressTooltipButton = new JideButton(noTooltipIcon);
        //supressTooltipButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
        //supressTooltipButton.setBorder(toolButtonBorder);
        supressTooltipButton.setAlignmentX(RIGHT_ALIGNMENT);
        supressTooltipButton.setToolTipText(DISABLE_POPUP_TOOLTIP);
        supressTooltipButton.setMaximumSize(new java.awt.Dimension(32, 32));
        supressTooltipButton.setMinimumSize(new java.awt.Dimension(32, 32));
        supressTooltipButton.setPreferredSize(new java.awt.Dimension(32, 32));
        supressTooltipButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                suppressTooltip = !suppressTooltip;
                if (suppressTooltip) {
                    //IGV.getInstance().getContentPane().getStatusBar().setMessage2("Data panel popup text is disabled");
                    supressTooltipButton.setIcon(tooltipIcon);
                    supressTooltipButton.setToolTipText(ENABLE_POPUP_TOOLTIP);
                } else {
                    //IGV.getInstance().getContentPane().getStatusBar().setMessage2("");
                    supressTooltipButton.setIcon(noTooltipIcon);
                    supressTooltipButton.setToolTipText(DISABLE_POPUP_TOOLTIP);
                }
            }
        });
        toolPanel.add(supressTooltipButton, JideBoxLayout.FIX);

        this.add(toolPanel);

        this.add(Box.createHorizontalGlue(), JideBoxLayout.VARY);

        zoomControl = new ZoomSliderPanel();

        // zoomControl.setAlignmentX(RIGHT_ALIGNMENT);
        zoomControl.setPreferredSize(new Dimension(200, 30));
        zoomControl.setMinimumSize(new Dimension(200, 30));
        zoomControl.setMaximumSize(new Dimension(200, 30));
        zoomControl.setToolTipText("Click + to zoom in,  - to zoom out.");
        zoomControl.setOpaque(false);
        this.add(zoomControl, JideBoxLayout.FIX);

        this.add(Box.createHorizontalStrut(20), JideBoxLayout.FIX);
    }

    /**
     * Method description
     *
     * @return
     */
    public GenomeListItem getGenomeSelectedInDropdown() {
        return (GenomeListItem) genomeComboBox.getSelectedItem();
    }

    private void adjustChromosomeDropdownWidth(int width) {

        int newWidth = (width > DEFAULT_CHROMOSOME_DROPDOWN_WIDTH)
                ? width : DEFAULT_CHROMOSOME_DROPDOWN_WIDTH;

        chromosomeComboBox.setMaximumSize(new java.awt.Dimension(newWidth, 35));
        chromosomeComboBox.setMinimumSize(new java.awt.Dimension(newWidth, 27));
        chromosomeComboBox.setPreferredSize(new java.awt.Dimension(newWidth, 16));
        revalidate();
    }

    private void homeButtonActionPerformed(java.awt.event.ActionEvent evt) {
        Genome genome = IGV.getInstance().getGenomeManager().getCurrentGenome();
        if (FrameManager.isGeneListMode()) {
            IGV.getInstance().setGeneList(null);
        }
        if (genome != null) {
            String chrName = genome.getHomeChromosome();
            getDefaultReferenceFrame().setChromosomeName(chrName);
            IGV.getInstance().getSession().getHistory().push(chrName, getDefaultReferenceFrame().getZoom());
            chromosomeComboBox.setSelectedItem(chrName);
            updateCurrentCoordinates();
            IGV.getInstance().chromosomeChangeEvent(chrName);
            IGV.getMainFrame().repaint();
        }
    }

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {
        //LRUCache.clearCaches();
        IGV.getInstance().doRefresh();
    }

    private void chromosomeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
        JComboBox combobox = (JComboBox) evt.getSource();
        final String chrName = (String) combobox.getSelectedItem();
        if (chrName != null) {

            if (!chrName.equals(getDefaultReferenceFrame().getChrName())) {
                NamedRunnable runnable = new NamedRunnable() {
                    public void run() {
                        getDefaultReferenceFrame().setChromosomeName(chrName);
                        getDefaultReferenceFrame().recordHistory();
                        updateCurrentCoordinates();
                        IGV.getInstance().chromosomeChangeEvent(chrName);
                        IGV.getMainFrame().repaint();
                        PreferenceManager.getInstance().setLastChromosomeViewed(chrName);
                    }

                    public String getName() {
                        return "Changed chromosome to: " + chrName;
                    }
                };

                LongRunningTask.submit(runnable);
            }
        }
    }

    private void goButtonActionPerformed(java.awt.event.ActionEvent evt) {    // GEN-FIRST:event_goButtonActionPerformed
        String searchText = searchTextField.getText();
        searchByLocus(searchText);
    }


    public void searchByLocus(final String searchText) {
        searchByLocus(searchText, true);
    }

    public void searchByLocus(final String searchText, boolean inBackground) {


        if (log.isDebugEnabled()) {
            log.debug("Enter search by locus: " + searchText);
        }

        if ((searchText != null) && (searchText.length() > 0)) {
            NamedRunnable runnable = new NamedRunnable() {
                public void run() {
                    searchTextField.setText(searchText);
                    (new SearchCommand(getDefaultReferenceFrame(), searchText)).execute();
                    chromosomeComboBox.setSelectedItem(getDefaultReferenceFrame().getChrName());
                }

                public String getName() {
                    return "Search: " + searchText;
                }
            };

            if (inBackground) {
                LongRunningTask.submit(runnable);
            } else {
                runnable.run();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Exit search by locus: " + searchText);
        }
    }


    private void roiToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {    // GEN-FIRST:event_roiToggleButtonActionPerformed
        if (roiToggleButton.isSelected()) {
            IGV.getInstance().beginROI(roiToggleButton);
        } else {
            IGV.getInstance().endROI();
        }
    }

    private class SearchHints extends ListDataIntelliHints<String> {

        public SearchHints(JTextComponent jTextComponent) {
            super(jTextComponent, new String[]{});
        }

        @Override
        public void acceptHint(Object context) {
            String text = (String) context;
            super.acceptHint(context);
            searchByLocus(text);
        }

        @Override
        public boolean updateHints(Object context) {
            String text = (String) context;
            if (text.length() <= 1) {
                return false;
            } else {
                List<NamedFeature> features = FeatureDB.getFeaturesList(text, SearchCommand.SEARCH_LIMIT);
                final List<SearchCommand.SearchResult> results = SearchCommand.getResults(features);
                Object[] list = SearchCommand.getSelectionList(results, false);
                if (list.length >= 1) {
                    this.setListData(list);
                    return true;
                }
            }
            return false;
        }
    }
}
