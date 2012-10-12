/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

/*
 * Created by JFormDesigner on Tue Mar 06 14:09:15 EST 2012
 */

package org.broad.igv.cbio;

import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.lists.GeneList;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.WaitCursorManager;
import org.broad.igv.ui.util.IndefiniteProgressMonitor;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.ui.util.ProgressBar;
import org.broad.igv.ui.util.UIUtilities;
import org.broad.igv.util.BrowserLauncher;
import org.broad.igv.util.HttpUtils;
import org.w3c.dom.Node;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Dialog for letting the user filter a GeneNetwork from
 * cBio.
 *
 * @author Jacob Silterra
 */
public class FilterGeneNetworkUI extends JDialog {

    private Logger log = Logger.getLogger(FilterGeneNetworkUI.class);

    private GeneList geneList;
    private List<AttributeFilter> filterRows = new ArrayList<AttributeFilter>(1);
    GeneNetwork network = null;

    private GraphListModel listModel;
    private Map<String, JTextField> thresholdsMap = new HashMap<String, JTextField>(5);


    private static List<String> columnNames;
    private static Map<Integer, String> columnNumToKeyMap;

    static {
        String[] firstLabels = {"Gene label", "Interactions"};
        columnNumToKeyMap = new HashMap<Integer, String>(GeneNetwork.attributeMap.size());
        columnNames = new ArrayList<String>(firstLabels.length + GeneNetwork.attributeMap.size());
        for (String label : firstLabels) {
            columnNames.add(label);
        }
        int ind = columnNames.size();
        for (String label : GeneNetwork.attributeMap.keySet()) {
            columnNumToKeyMap.put(ind, label);
            ind++;

            label = label.replace('_', ' ');
            label = label.replace("PERCENT", "%");
            columnNames.add(label);

        }

    }

    public FilterGeneNetworkUI(Frame owner, GeneList geneList) {
        super(owner);
        this.geneList = geneList;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && network == null) {
            loadcBioData(this.geneList.getLoci());
        } else {
            super.setVisible(visible);
        }
    }

    private void loadcBioData(final List<String> geneLoci) {

        final IndefiniteProgressMonitor indefMonitor = new IndefiniteProgressMonitor(60);
        final ProgressBar progressBar = ProgressBar.showProgressDialog((Frame) getOwner(), "Loading cBio data...", indefMonitor, true);
        progressBar.setIndeterminate(true);
        indefMonitor.start();

        final Runnable showUI = new Runnable() {
            @Override
            public void run() {
                initComponents();
                initComponentData();
                setVisible(true);
            }
        };

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {

                WaitCursorManager.CursorToken token = null;

                try {
                    token = WaitCursorManager.showWaitCursor();
                    network = GeneNetwork.getFromCBIO(geneLoci);
                    if (network.vertexSet().size() == 0) {
                        MessageUtils.showMessage("No results found for " + HttpUtils.buildURLString(geneLoci, ", "));
                    } else {
                        network.annotateAll(IGV.getInstance().getAllTracks(false));
                        UIUtilities.invokeOnEventThread(showUI);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                    MessageUtils.showMessage("Error loading data: " + e.getMessage());
                } finally {
                    WaitCursorManager.removeWaitCursor(token);

                    if (progressBar != null) {
                        progressBar.close();
                        indefMonitor.stop();
                    }
                }
            }
        };

        // If we're on the dispatch thread spawn a worker, otherwise just execute.
        if (SwingUtilities.isEventDispatchThread()) {
            SwingWorker worker = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    runnable.run();
                    return null;
                }
            };

            worker.execute();
        } else {
            runnable.run();
        }
    }


    private void initThresholdsMap() {
        thresholdsMap.put(PreferenceManager.CBIO_MUTATION_THRESHOLD, mutInput);
        thresholdsMap.put(PreferenceManager.CBIO_AMPLIFICATION_THRESHOLD, ampInput);
        thresholdsMap.put(PreferenceManager.CBIO_DELETION_THRESHOLD, delInput);
        thresholdsMap.put(PreferenceManager.CBIO_EXPRESSION_UP_THRESHOLD, expUpInput);
        thresholdsMap.put(PreferenceManager.CBIO_EXPRESSION_DOWN_THRESHOLD, expDownInput);
    }

    /**
     * Load data into visual components. This should be called
     * AFTER loading network.
     */
    private void initComponentData() {
        add();

        initThresholdsMap();

        loadThresholds();

        listModel = new GraphListModel();
        geneTable.setModel(listModel);
        applySoftFilters();
    }

    private void remove(AttributeFilter row) {
        contentPane.remove(row.getPanel());
        filterRows.remove(row);
        int numRows = filterRows.size();
        filterRows.get(numRows - 1).setIsLast(true);
        filterRows.get(0).setShowDel(numRows >= 2);
        validateTree();
    }


    private void add() {
        final AttributeFilter row = new AttributeFilter();

        row.getDelRow().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remove(row);
            }
        });

        row.getAddRow().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                add();
            }
        });
        contentPane.add(row.getPanel());

        //We want to refresh the
        RefreshListener listener = new RefreshListener();

        row.getAttrName().addActionListener(listener);
        for (JTextField text : new JTextField[]{row.minVal, row.maxVal}) {
            text.addActionListener(listener);
            text.addFocusListener(listener);
        }


        //Set the status of being last
        if (filterRows.size() >= 1) {
            filterRows.get(filterRows.size() - 1).setIsLast(false);
        }
        filterRows.add(row);

        int numRows = filterRows.size();
        filterRows.get(numRows - 1).setIsLast(true);
        filterRows.get(0).setShowDel(numRows >= 2);

        validateTree();

    }

    private void cancelButtonActionPerformed(ActionEvent e) {
        setVisible(false);
    }

    private void applySoftFilters() {
        network.clearAllFilters();

        //TODO This is only AND, should also include OR
        for (AttributeFilter filter : this.filterRows) {
            String filt_el = (String) filter.getAttrName().getSelectedItem();
            if (GeneNetwork.attributeMap.containsKey(filt_el) || GeneNetwork.PERCENT_ALTERED.equals(filt_el)) {
                float min = Float.parseFloat(filter.minVal.getText());
                float max = Float.parseFloat(filter.maxVal.getText());
                network.filterNodesRange(filt_el, min / 100, max / 100);
            }
        }
        if (!keepIsolated.isSelected()) {
            network.pruneGraph();
        }

        this.listModel.markDirty();
    }

    /**
     * TODO This should run on a separate thread
     */
    private void showNetwork() {

        try {
            String url = network.outputForcBioView();
            url = "file://" + url;
            BrowserLauncher.openURL(url);
        } catch (IOException err) {
            MessageUtils.showMessage("Error opening network for viewing. " + err.getMessage());
        }
    }

    /**
     * Sorting of the rows is done on the view, not the underlying model,
     * so we must convert. See {@link TableRowSorter}
     *
     * @return int[] of model indices which are selected
     */
    private int[] getModelIndices() {
        int[] selection = geneTable.getSelectedRows();
        for (int i = 0; i < selection.length; i++) {
            selection[i] = geneTable.convertRowIndexToModel(selection[i]);
        }
        return selection;
    }

    private void okButtonActionPerformed(ActionEvent e) {
        //If any rows are selected, we only keep those
        //We lookup by name, because table could get sorted
        int[] keepRows = getModelIndices();
        if (keepRows.length > 0) {
            final Set<Node> keepNodes = new HashSet<Node>(keepRows.length);
            GraphListModel model = (GraphListModel) geneTable.getModel();
            List<Node> vertices = model.getVertices();
            for (Integer loc : keepRows) {
                keepNodes.add(vertices.get(loc));
            }

            Predicate<Node> selectedPredicated = new Predicate<Node>() {

                @Override
                public boolean evaluate(Node object) {
                    return keepNodes.contains(object);
                }
            };
            network.filterNodes(selectedPredicated);
        }

        setVisible(false);
        showNetwork();
    }

    private void addRowActionPerformed(ActionEvent e) {
        add();
    }

    /**
     * Refresh the view based on filter input.
     */
    private void refreshFilters() {
        this.applySoftFilters();
        listModel.markDirty();
        this.validateTree();
    }

    private void refFilterActionPerformed(ActionEvent e) {
        refreshFilters();
    }

    private boolean saveThresholds() {
        try {
            for (Map.Entry<String, JTextField> entry : thresholdsMap.entrySet()) {
                float value = Float.parseFloat(entry.getValue().getText());
                PreferenceManager.getInstance().put(entry.getKey(), "" + value);
            }
        } catch (NumberFormatException e) {
            MessageUtils.showMessage("Inputs must be numeric. " + e.getMessage());
            return false;
        }
        return true;
    }

    private void loadThresholds() {
        for (Map.Entry<String, JTextField> entry : thresholdsMap.entrySet()) {
            String value = PreferenceManager.getInstance().get(entry.getKey());
            entry.getValue().setText(value);
        }
    }

    private void tabbedPaneStateChanged(ChangeEvent e) {
        int thresholdTabNum = tabbedPane.indexOfTab("Thresholds");
        if (thresholdTabNum < 0) {
            //Component not built yet
            return;
        }
        Component thresholdTab = tabbedPane.getComponentAt(thresholdTabNum);
        if (!tabbedPane.getSelectedComponent().equals(thresholdTab) && !saveThresholds()) {
            tabbedPane.setSelectedComponent(thresholdTab);
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        tabbedPane = new JTabbedPane();
        dialogPane = new JPanel();
        panel1 = new JPanel();
        addRow = new JButton();
        contentPane = new JPanel();
        buttonBar = new JPanel();
        keepIsolated = new JCheckBox();
        okButton = new JButton();
        refFilter = new JButton();
        cancelButton = new JButton();
        helpButton = new JButton();
        scrollPane1 = new JScrollPane();
        geneTable = new JTable();
        thresholds = new JPanel();
        contentPanel = new JPanel();
        label1 = new JLabel();
        mutInput = new JTextField();
        label2 = new JLabel();
        ampInput = new JTextField();
        label3 = new JLabel();
        delInput = new JTextField();
        label4 = new JLabel();
        expUpInput = new JTextField();
        label7 = new JLabel();
        expDownInput = new JTextField();

        //======== this ========
        setMinimumSize(new Dimension(600, 22));
        Container contentPane2 = getContentPane();
        contentPane2.setLayout(new BorderLayout());

        //======== tabbedPane ========
        {
            tabbedPane.setPreferredSize(new Dimension(550, 346));
            tabbedPane.setMinimumSize(new Dimension(550, 346));
            tabbedPane.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    tabbedPaneStateChanged(e);
                }
            });

            //======== dialogPane ========
            {
                dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
                dialogPane.setMinimumSize(new Dimension(443, 300));
                dialogPane.setPreferredSize(new Dimension(443, 300));
                dialogPane.setLayout(new GridBagLayout());
                ((GridBagLayout) dialogPane.getLayout()).columnWidths = new int[]{0, 0};
                ((GridBagLayout) dialogPane.getLayout()).rowHeights = new int[]{0, 0, 0, 0, 0};
                ((GridBagLayout) dialogPane.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
                ((GridBagLayout) dialogPane.getLayout()).rowWeights = new double[]{0.0, 0.0, 1.0, 0.0, 1.0E-4};

                //======== panel1 ========
                {
                    panel1.setLayout(new GridBagLayout());
                    ((GridBagLayout) panel1.getLayout()).columnWidths = new int[]{0, 0, 0};
                    ((GridBagLayout) panel1.getLayout()).rowHeights = new int[]{0, 0};
                    ((GridBagLayout) panel1.getLayout()).columnWeights = new double[]{0.0, 0.0, 1.0E-4};
                    ((GridBagLayout) panel1.getLayout()).rowWeights = new double[]{0.0, 1.0E-4};

                    //---- addRow ----
                    addRow.setText("Add Filter");
                    addRow.setMaximumSize(new Dimension(200, 28));
                    addRow.setMinimumSize(new Dimension(100, 28));
                    addRow.setPreferredSize(new Dimension(150, 28));
                    addRow.setVisible(false);
                    addRow.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addRowActionPerformed(e);
                        }
                    });
                    panel1.add(addRow, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));
                }
                dialogPane.add(panel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //======== contentPane ========
                {
                    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
                }
                dialogPane.add(contentPane, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //======== buttonBar ========
                {
                    buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                    buttonBar.setLayout(new GridBagLayout());
                    ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[]{0, 85, 85, 80};
                    ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[]{1.0, 0.0, 0.0, 0.0};

                    //---- keepIsolated ----
                    keepIsolated.setText("Keep Isolated Genes");
                    keepIsolated.setVisible(false);
                    buttonBar.add(keepIsolated, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 5), 0, 0));

                    //---- okButton ----
                    okButton.setText("View Network");
                    okButton.setToolTipText("Display the network in a web browser");
                    okButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            okButtonActionPerformed(e);
                        }
                    });
                    buttonBar.add(okButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 5), 0, 0));

                    //---- refFilter ----
                    refFilter.setText("Refresh Filter");
                    refFilter.setVisible(false);
                    refFilter.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            refFilterActionPerformed(e);
                        }
                    });
                    buttonBar.add(refFilter, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 5), 0, 0));

                    //---- cancelButton ----
                    cancelButton.setText("Cancel");
                    cancelButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            cancelButtonActionPerformed(e);
                        }
                    });
                    buttonBar.add(cancelButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 5), 0, 0));

                    //---- helpButton ----
                    helpButton.setText("Help");
                    helpButton.setVisible(false);
                    buttonBar.add(helpButton, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));
                }
                dialogPane.add(buttonBar, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

                //======== scrollPane1 ========
                {

                    //---- geneTable ----
                    geneTable.setAutoCreateRowSorter(true);
                    scrollPane1.setViewportView(geneTable);
                }
                dialogPane.add(scrollPane1, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
            }
            tabbedPane.addTab("Filter", dialogPane);


            //======== thresholds ========
            {
                thresholds.setBorder(new EmptyBorder(12, 12, 12, 12));
                thresholds.setPreferredSize(new Dimension(550, 196));
                thresholds.setMinimumSize(new Dimension(550, 196));
                thresholds.setLayout(new BorderLayout());

                //======== contentPanel ========
                {
                    contentPanel.setLayout(new GridBagLayout());
                    ((GridBagLayout) contentPanel.getLayout()).columnWidths = new int[]{0, 0, 0, 0, 0, 0};
                    ((GridBagLayout) contentPanel.getLayout()).rowHeights = new int[]{0, 0, 0, 0, 0};
                    ((GridBagLayout) contentPanel.getLayout()).columnWeights = new double[]{1.0, 0.0, 0.0, 1.0, 1.0, 1.0E-4};
                    ((GridBagLayout) contentPanel.getLayout()).rowWeights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0E-4};

                    //---- label1 ----
                    label1.setText("Mutation:");
                    label1.setHorizontalAlignment(SwingConstants.RIGHT);
                    label1.setLabelFor(mutInput);
                    contentPanel.add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 10, 5), 0, 0));

                    //---- mutInput ----
                    mutInput.setText("1");
                    mutInput.setAutoscrolls(false);
                    mutInput.setMinimumSize(new Dimension(34, 28));
                    mutInput.setPreferredSize(new Dimension(45, 28));
                    contentPanel.add(mutInput, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 10, 5), 0, 0));

                    //---- label2 ----
                    label2.setText("Amplification:");
                    label2.setHorizontalAlignment(SwingConstants.RIGHT);
                    label2.setLabelFor(ampInput);
                    contentPanel.add(label2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 10, 5), 0, 0));

                    //---- ampInput ----
                    ampInput.setText("0.7");
                    ampInput.setMinimumSize(new Dimension(34, 28));
                    ampInput.setPreferredSize(new Dimension(45, 28));
                    contentPanel.add(ampInput, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 10, 5), 0, 0));

                    //---- label3 ----
                    label3.setText("Deletion:");
                    label3.setHorizontalAlignment(SwingConstants.RIGHT);
                    label3.setLabelFor(delInput);
                    contentPanel.add(label3, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 10, 5), 0, 0));

                    //---- delInput ----
                    delInput.setText("0.7");
                    delInput.setMinimumSize(new Dimension(34, 28));
                    delInput.setPreferredSize(new Dimension(45, 28));
                    delInput.setMaximumSize(new Dimension(50, 2147483647));
                    contentPanel.add(delInput, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 10, 5), 0, 0));

                    //---- label4 ----
                    label4.setText("Expression Up:");
                    label4.setHorizontalAlignment(SwingConstants.RIGHT);
                    label4.setLabelFor(expUpInput);
                    contentPanel.add(label4, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 10, 5), 0, 0));

                    //---- expUpInput ----
                    expUpInput.setText("0.1");
                    expUpInput.setMinimumSize(new Dimension(34, 28));
                    expUpInput.setPreferredSize(new Dimension(35, 28));
                    contentPanel.add(expUpInput, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 10, 5), 0, 0));

                    //---- label7 ----
                    label7.setText("Expression Down:");
                    label7.setHorizontalAlignment(SwingConstants.RIGHT);
                    label7.setLabelFor(expDownInput);
                    contentPanel.add(label7, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 10, 5), 0, 0));

                    //---- expDownInput ----
                    expDownInput.setText("0.1");
                    expDownInput.setPreferredSize(new Dimension(45, 28));
                    expDownInput.setMinimumSize(new Dimension(34, 28));
                    expDownInput.setMaximumSize(new Dimension(50, 2147483647));
                    contentPanel.add(expDownInput, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 10, 5), 0, 0));
                }
                thresholds.add(contentPanel, BorderLayout.CENTER);
            }
            tabbedPane.addTab("Thresholds", thresholds);

        }
        contentPane2.add(tabbedPane, BorderLayout.NORTH);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JTabbedPane tabbedPane;
    private JPanel dialogPane;
    private JPanel panel1;
    private JButton addRow;
    private JPanel contentPane;
    private JPanel buttonBar;
    private JCheckBox keepIsolated;
    private JButton okButton;
    private JButton refFilter;
    private JButton cancelButton;
    private JButton helpButton;
    private JScrollPane scrollPane1;
    private JTable geneTable;
    private JPanel thresholds;
    private JPanel contentPanel;
    private JLabel label1;
    private JTextField mutInput;
    private JLabel label2;
    private JTextField ampInput;
    private JLabel label3;
    private JTextField delInput;
    private JLabel label4;
    private JTextField expUpInput;
    private JLabel label7;
    private JTextField expDownInput;
    // JFormDesigner - End of variables declaration  //GEN-END:variables


    private class GraphListModel extends AbstractTableModel {

        private List<Node> vertices = null;

        private List<Node> getVertices() {
            if (vertices == null) {
                Set<Node> nodes = network.vertexSetFiltered();
                vertices = Arrays.asList(nodes.toArray(new Node[0]));
            }
            return vertices;

            //Collections.sort(vertexNames);
        }

        public void markDirty() {
            this.vertices = null;
            this.fireTableStructureChanged();
        }

        @Override
        public int getRowCount() {
            return getVertices().size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.size();
        }

        public String getColumnName(int col) {
            return columnNames.get(col);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {

            Node n = getVertices().get(rowIndex);
            String nm = GeneNetwork.getNodeKeyData(n, "label");
            switch (columnIndex) {
                case 0:
                    return nm;
                case 1:
                    return network.edgesOfFiltered(n).size();
                default:
                    String key = columnNumToKeyMap.get(columnIndex);
                    if (key == null) {
                        return null;
                    }
                    String val = GeneNetwork.getNodeKeyData(n, key);
                    if ("nan".equalsIgnoreCase(val)) {
                        return null;
                    }
                    //Change from fraction to percent
                    double dPerc = Double.parseDouble(val) * 100;
                    if (dPerc == 0.0d) return "0.0";
                    //If above 1, just show integer. If small, show in exponential format
                    String fmt = "%2.1f";
                    if (dPerc < 0.1d) {
                        fmt = "%2.1e";
                    }

                    String sVal = String.format(fmt, dPerc);
                    return sVal;

            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    private class RefreshListener implements FocusListener, ActionListener {

        @Override
        public void focusGained(FocusEvent e) {
            //pass
        }

        @Override
        public void focusLost(FocusEvent e) {
            refreshFilters();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            refreshFilters();
        }
    }
}
