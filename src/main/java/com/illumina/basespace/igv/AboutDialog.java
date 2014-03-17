package com.illumina.basespace.igv;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class AboutDialog extends JDialog
{
    private JLabel lblVersion;

    private final JPanel contentPanel = new JPanel();
    /**
     * Create the dialog.
     */
    public AboutDialog()
    {
        setTitle("About BaseSpace IGV");
        setBounds(100, 100, 244, 138);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        GridBagLayout gbl_contentPanel = new GridBagLayout();
        contentPanel.setLayout(gbl_contentPanel);
        {
            JLabel lblVersion = new JLabel("Version:");
            GridBagConstraints gbc_lblVersion = new GridBagConstraints();
            gbc_lblVersion.insets = new Insets(10, 10, 0, 5);
            gbc_lblVersion.gridx = 0;
            gbc_lblVersion.gridy = 0;
            contentPanel.add(lblVersion, gbc_lblVersion);
        }
        {
            lblVersion = new JLabel("TBD");
            String version = extractVersion();
            if (version != null)lblVersion.setText(version);
            
            GridBagConstraints gbc_lblTbd = new GridBagConstraints();
            gbc_lblTbd.insets = new Insets(10, 0, 0, 0);
            gbc_lblTbd.gridx = 1;
            gbc_lblTbd.gridy = 0;
            contentPanel.add(lblVersion, gbc_lblTbd);
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dispose();
                    }
                });
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
        }
    }

    protected String extractVersion()
    {
        InputStream is = null;
        try
        {
            is = this.getClass().getResourceAsStream("/integration.properties");
            if (is != null)
            {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("integration.version");
            }
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (Throwable t)
                {
                    
                }
            }
        }
        
        return null;
        
    }
}
