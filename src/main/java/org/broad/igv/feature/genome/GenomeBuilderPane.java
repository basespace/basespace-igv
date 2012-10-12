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
package org.broad.igv.feature.genome;

import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.PreferenceManager;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.util.FileDialogUtils;

import javax.swing.*;
import java.io.File;
import java.io.Serializable;
import java.util.Collection;

/**
 * @author eflakes
 */
public class GenomeBuilderPane extends javax.swing.JPanel implements Serializable {

    private static Logger logger = Logger.getLogger(GenomeBuilderPane.class);
    private String genomeArchiveLocation;
    private String genomeFilename;
    GenomeImporter importer;
    IGV igv;


    public GenomeBuilderPane() {
        initComponents();
        importer = new GenomeImporter();
    }

    public void setIgv(IGV igv) {
        this.igv = igv;
    }

    public String getCytobandFileName() {
        String cytobandFile = cytobandFileTextField.getText();
        if (cytobandFile != null && cytobandFile.trim().equals("")) {
            cytobandFile = null;
        }
        return cytobandFile;
    }

    public String getRefFlatFileName() {
        String refFlatFile = refFlatFileTextField.getText();
        if (refFlatFile != null && refFlatFile.trim().equals("")) {
            refFlatFile = null;
        }
        return refFlatFile;
    }

    public String getFastaFileName() {
        String fastaFile = fastaFileTextField.getText();
        if (fastaFile != null && fastaFile.trim().equals("")) {
            fastaFile = null;
        }
        return fastaFile;
    }

    public String getChrAliasFileName() {
        String chrAliasFile = chrAliasField.getText();
        if (chrAliasFile != null && chrAliasFile.trim().equals("")) {
            chrAliasFile = null;
        }
        return chrAliasFile;
    }


    public String getGenomeId() {
        return idField.getText();
    }

//    public String getSequenceURL() {
//        return sequenceURLField.getText();
//    }


    public String getGenomeDisplayName() {
        String name = genomeDisplayNameTextField.getText();
        if (name != null && name.trim().equals("")) {
            name = null;
        } else {
            name = name.trim();
        }
        return name;
    }

    public String getGenomeArchiveLocation() {
        if (genomeArchiveLocation != null && genomeArchiveLocation.trim().equals("")) {
            genomeArchiveLocation = null;
        }
        return genomeArchiveLocation;
    }

    public String getArchiveFileName() {

        if (genomeFilename == null) {
            genomeFilename = getGenomeId() + Globals.GENOME_FILE_EXTENSION;
        }
        return genomeFilename;
    }


    protected File showGenomeArchiveDirectoryChooser() {

        File directory = PreferenceManager.getInstance().getLastGenomeImportDirectory();
        File archiveName = new File(getGenomeId() + Globals.GENOME_FILE_EXTENSION);
        File file = FileDialogUtils.chooseFile("Save Genome File", directory, archiveName, FileDialogUtils.SAVE);

        if (file != null) {

            genomeFilename = file.getName();
            if (genomeFilename != null) {
                if (!genomeFilename.endsWith(Globals.GENOME_FILE_EXTENSION)) {
                    genomeFilename += Globals.GENOME_FILE_EXTENSION;
                    file = new File(file.getParentFile(), genomeFilename);
                }
                genomeArchiveLocation = file.getParentFile().getAbsolutePath();
            }
        }
        return file;
    }

    public boolean validateSelection() {

        try {

            if (!isIdValid()) {
                return false;
            }

            if (!isGenomeDisplayNameValid()) {
                genomeDisplayNameTextField.setText(null);
                return false;
            }

            if (!isFASTAFileValid()) {
                JOptionPane.showMessageDialog(this, "A fasta file is required!");
                return false;
            }

        } catch (Exception e) {
            logger.error("Error during Genome Builder validation!", e);
            return false;
        }
        return true;
    }

    private boolean isFASTAFileValid() {

        String file = fastaFileTextField.getText();
        return file != null && file.trim().length() > 0;
    }

    private Boolean isIdValid() {

        String id = getGenomeId();
        if (id == null || id.trim().length() == 0) {
            JOptionPane.showMessageDialog(this, "Genome ID is required");
            return false;
        }

        Collection<String> inUseIds = igv.getGenomeIds();
        if (inUseIds.contains(id)) {
            JOptionPane.showMessageDialog(this,
                    "The genome ID '" + id + "' is already in use - please select another!");
            return false;
        }
        return true;
    }

    private boolean isGenomeDisplayNameValid() {

        String displayName = getGenomeDisplayName();
        if (displayName == null || displayName.trim().length() == 0) {
            JOptionPane.showMessageDialog(this, "Genome name is required");
            return false;
        }

        Collection<String> inUseDisplayNames = igv.getGenomeDisplayNames();

        if (inUseDisplayNames.contains(displayName)) {
            JOptionPane.showMessageDialog(this,
                    "The genome name '" + displayName + "' is already in use - please select another!");
            return false;
        }
        return true;
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    private void initComponents() {
        panel3 = new JPanel();
        panel2 = new JPanel();
        genomeDisplayNameLabel2 = new JLabel();
        idField = new JTextField();
        genomeDisplayNameLabel = new JLabel();
        genomeDisplayNameTextField = new JTextField();
        fastaFileLabel = new JLabel();
        fastaFileTextField = new JTextField();
        fastaFileButton = new JButton();
        panel1 = new JPanel();
        cytobandFileLabel = new JLabel();
        cytobandFileTextField = new JTextField();
        cytobandFileButton = new JButton();
        refFlatFileLabel = new JLabel();
        refFlatFileTextField = new JTextField();
        refFlatFileButton = new JButton();
        refFlatFileLabel2 = new JLabel();
        chrAliasField = new JTextField();
        chrAliasButton = new JButton();
        vSpacer2 = new JPanel(null);

        //======== this ========
        setFont(new Font("Tahoma", Font.ITALIC, 12));
        setMaximumSize(new Dimension(900, 500));
        setMinimumSize(new Dimension(400, 200));
        setPreferredSize(new Dimension(700, 200));
        setLayout(new BorderLayout());

        //======== panel3 ========
        {
            panel3.setBorder(new EmptyBorder(20, 20, 20, 20));
            panel3.setLayout(new BoxLayout(panel3, BoxLayout.Y_AXIS));

            //======== panel2 ========
            {
                panel2.setBorder(new EmptyBorder(0, 0, 30, 0));
                panel2.setLayout(null);

                //---- genomeDisplayNameLabel2 ----
                genomeDisplayNameLabel2.setText("Unique identifier");
                genomeDisplayNameLabel2.setToolTipText("Unique identifier (e.g. hg18)");
                genomeDisplayNameLabel2.setMaximumSize(new Dimension(84, 16));
                genomeDisplayNameLabel2.setMinimumSize(new Dimension(84, 16));
                genomeDisplayNameLabel2.setPreferredSize(new Dimension(14, 16));
                panel2.add(genomeDisplayNameLabel2);
                genomeDisplayNameLabel2.setBounds(15, 22, 125, 28);

                //---- idField ----
                idField.setToolTipText("A uniqe identifier for the genome");
                panel2.add(idField);
                idField.setBounds(145, 22, 548, 29);

                //---- genomeDisplayNameLabel ----
                genomeDisplayNameLabel.setText("Descriptive name");
                panel2.add(genomeDisplayNameLabel);
                genomeDisplayNameLabel.setBounds(15, 56, 175, 28);

                //---- genomeDisplayNameTextField ----
                genomeDisplayNameTextField.setToolTipText("The user-readable name of the genome");
                genomeDisplayNameTextField.setPreferredSize(new Dimension(400, 28));
                genomeDisplayNameTextField.setMinimumSize(new Dimension(25, 28));
                panel2.add(genomeDisplayNameTextField);
                genomeDisplayNameTextField.setBounds(145, 56, 548, 29);

                //---- fastaFileLabel ----
                fastaFileLabel.setText("FASTA file");
                panel2.add(fastaFileLabel);
                fastaFileLabel.setBounds(15, 88, 175, 29);

                //---- fastaFileTextField ----
                fastaFileTextField.setToolTipText("A FASTA data file");
                fastaFileTextField.setPreferredSize(new Dimension(400, 28));
                fastaFileTextField.setMinimumSize(new Dimension(25, 28));
                panel2.add(fastaFileTextField);
                fastaFileTextField.setBounds(145, 88, 548, 29);

                //---- fastaFileButton ----
                fastaFileButton.setLabel("...");
                fastaFileButton.setText("Browse");
                fastaFileButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        fastaFileButtonActionPerformed(e);
                    }
                });
                panel2.add(fastaFileButton);
                fastaFileButton.setBounds(698, 88, 83, fastaFileButton.getPreferredSize().height);
            }
            panel3.add(panel2);

            //======== panel1 ========
            {
                panel1.setBorder(new TitledBorder(null, "Optional", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                    new Font("Lucida Grande", Font.BOLD, 13)));
                panel1.setLayout(null);

                //---- cytobandFileLabel ----
                cytobandFileLabel.setText("Cytoband file");
                panel1.add(cytobandFileLabel);
                cytobandFileLabel.setBounds(15, 25, 105, 29);

                //---- cytobandFileTextField ----
                cytobandFileTextField.setToolTipText("A cytoband data file");
                cytobandFileTextField.setPreferredSize(new Dimension(400, 28));
                cytobandFileTextField.setMinimumSize(new Dimension(25, 28));
                panel1.add(cytobandFileTextField);
                cytobandFileTextField.setBounds(145, 22, 548, 29);

                //---- cytobandFileButton ----
                cytobandFileButton.setLabel("...");
                cytobandFileButton.setText("Browse");
                cytobandFileButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        cytobandFileButtonActionPerformed(e);
                    }
                });
                panel1.add(cytobandFileButton);
                cytobandFileButton.setBounds(698, 22, 83, cytobandFileButton.getPreferredSize().height);

                //---- refFlatFileLabel ----
                refFlatFileLabel.setText("Gene file");
                panel1.add(refFlatFileLabel);
                refFlatFileLabel.setBounds(15, 56, 84, 29);

                //---- refFlatFileTextField ----
                refFlatFileTextField.setToolTipText("An annotation file");
                refFlatFileTextField.setPreferredSize(new Dimension(400, 28));
                refFlatFileTextField.setMinimumSize(new Dimension(25, 28));
                panel1.add(refFlatFileTextField);
                refFlatFileTextField.setBounds(145, 56, 548, 29);

                //---- refFlatFileButton ----
                refFlatFileButton.setLabel("...");
                refFlatFileButton.setText("Browse");
                refFlatFileButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        refFlatFileButtonActionPerformed(e);
                    }
                });
                panel1.add(refFlatFileButton);
                refFlatFileButton.setBounds(698, 56, 83, refFlatFileButton.getPreferredSize().height);

                //---- refFlatFileLabel2 ----
                refFlatFileLabel2.setText("Alias file");
                panel1.add(refFlatFileLabel2);
                refFlatFileLabel2.setBounds(15, 90, 84, 29);

                //---- chrAliasField ----
                chrAliasField.setPreferredSize(new Dimension(400, 28));
                chrAliasField.setMinimumSize(new Dimension(25, 28));
                panel1.add(chrAliasField);
                chrAliasField.setBounds(145, 90, 548, 29);

                //---- chrAliasButton ----
                chrAliasButton.setLabel("...");
                chrAliasButton.setText("Browse");
                chrAliasButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        chrAliasButtonActionPerformed(e);
                    }
                });
                panel1.add(chrAliasButton);
                chrAliasButton.setBounds(698, 90, 83, chrAliasButton.getPreferredSize().height);
                panel1.add(vSpacer2);
                vSpacer2.setBounds(6, 124, 84, 20);
            }
            panel3.add(panel1);
        }
        add(panel3, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void chrAliasButtonActionPerformed(ActionEvent e) {
        File directory = PreferenceManager.getInstance().getDefineGenomeInputDirectory();
        File file = FileDialogUtils.chooseFile("Select Chromosome Alias File", directory, FileDialogUtils.LOAD);
        if (file != null) {
            chrAliasField.setText(file.getAbsolutePath());
            PreferenceManager.getInstance().setDefineGenomeInputDirectory(file.getParentFile());
        }
    }


    private void cytobandFileButtonActionPerformed(java.awt.event.ActionEvent evt) {
        File directory = PreferenceManager.getInstance().getDefineGenomeInputDirectory();
        File file = FileDialogUtils.chooseFile("Select Cytoband File", directory, FileDialogUtils.LOAD);
        if (file != null) {
            cytobandFileTextField.setText(file.getAbsolutePath());
            PreferenceManager.getInstance().setDefineGenomeInputDirectory(file.getParentFile());
        }
    }

    private void refFlatFileButtonActionPerformed(java.awt.event.ActionEvent evt) {
        File directory = PreferenceManager.getInstance().getDefineGenomeInputDirectory();
        File file = FileDialogUtils.chooseFile("Select Annotation File", directory, FileDialogUtils.LOAD);
        if (file != null) {
            refFlatFileTextField.setText(file.getAbsolutePath());
            PreferenceManager.getInstance().setDefineGenomeInputDirectory(file.getParentFile());
        }
    }

    private void fastaFileButtonActionPerformed(java.awt.event.ActionEvent evt) {
        File directory = PreferenceManager.getInstance().getDefineGenomeInputDirectory();
        File file = FileDialogUtils.chooseFileOrDirectory("Select Fasta File", directory, null, FileDialogUtils.LOAD);
        if (file != null) {
            fastaFileTextField.setText(file.getAbsolutePath());
            PreferenceManager.getInstance().setDefineGenomeInputDirectory(file.getParentFile());
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel panel3;
    private JPanel panel2;
    private JLabel genomeDisplayNameLabel2;
    private JTextField idField;
    private JLabel genomeDisplayNameLabel;
    private JTextField genomeDisplayNameTextField;
    private JLabel fastaFileLabel;
    private JTextField fastaFileTextField;
    private JButton fastaFileButton;
    private JPanel panel1;
    private JLabel cytobandFileLabel;
    private JTextField cytobandFileTextField;
    private JButton cytobandFileButton;
    private JLabel refFlatFileLabel;
    private JTextField refFlatFileTextField;
    private JButton refFlatFileButton;
    private JLabel refFlatFileLabel2;
    private JTextField chrAliasField;
    private JButton chrAliasButton;
    private JPanel vSpacer2;
    // End of variables declaration//GEN-END:variables

}
