package com.illumina.desktop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;


public class DetailDialog extends javax.swing.JDialog implements ClipboardOwner
{
    private static Image errorSmall = ImageProvider.instance().getImage("error16x16.png");
    private static Icon errorLarge = ImageProvider.instance().getIcon("error64x64.png");
    private static Image infoSmall = ImageProvider.instance().getImage("info16x16.png");
    private static Icon infoLarge = ImageProvider.instance().getIcon("info64x64.png");
    private static Image warningSmall = ImageProvider.instance().getImage("warning.gif");
    private static Icon warningLarge = ImageProvider.instance().getIcon("warning64x64.png");
    private NotifyType notifyType = NotifyType.Information;

    public DetailDialog(java.awt.Frame parent, boolean modal, String summary, Throwable t)
    {
        super(parent, modal);
        this.notifyType = NotifyType.Error;
        initDialog(summary, null, t);
    }
    
    public DetailDialog()
    {
        this.notifyType = NotifyType.Error;
        initDialog("This is a summary message", null, null);
    }

    public DetailDialog(java.awt.Frame parent, boolean modal, Throwable t)
    {
        this(parent, modal, null, t);
    }

    public DetailDialog(java.awt.Frame parent, boolean modal, String summary, String detail, NotifyType type)
    {
        super(parent, modal);
        this.notifyType = type;
        initDialog(summary, detail, null);
    }

    public DetailDialog(JDialog parent, boolean modal, String summary, Throwable t)
    {
        super(parent, modal);
        this.notifyType = NotifyType.Error;
        initDialog(summary, null, t);
    }

    public DetailDialog(JDialog parent, boolean modal, Throwable t)
    {
        this(parent, modal, null, t);
    }

    public DetailDialog(JDialog parent, boolean modal, String summary, String detail, NotifyType type)
    {
        super(parent, modal);
        this.notifyType = type;
        initDialog(summary, detail, null);
    }

    private void initDialog(String summary, String detail, Throwable e)
    {
    	Throwable t = e;
        Action action = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                btnOK.doClick();
            }
        };

        getRootPane().registerKeyboardAction(action, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        initComponents();
        Icon largeIcon = infoLarge;
        Image smallImage = infoSmall;
        String title = "Information";

        switch (notifyType)
        {
            case Warning:
                largeIcon = warningLarge;
                smallImage = warningSmall;
                title = "Warning";
                break;
            case Error:
                largeIcon = errorLarge;
                smallImage = errorSmall;
                title = "Error";
                if (summary == null)
                {
                    summary = t.getMessage() != null && t.getMessage().length() > 0 ? t.getMessage() : "An exception occured (See Detail)";
                }
                detail = getThrowableDetail(t);
                break;
        }
        setIconImage(smallImage);
        setHeaderIcon(largeIcon);
        setTitle(title);
        setSummaryText(summary);
        setDetailText(detail);

        btnDetailActionPerformed(null);

        EventQueue.invokeLater(new Runnable()
        {

            @Override
            public void run()
            {
                summaryPane.scrollRectToVisible(new Rectangle(0, 0));
                txtSummary.setCaretPosition(0);
                detailPane.scrollRectToVisible(new Rectangle(0, 0));
                txtDetail.setCaretPosition(0);
            }
        });
    }

    public void setVisible(boolean visible)
    {
        super.setVisible(visible);

    }

    public void setSummaryText(String text)
    {
        txtSummary.setText(text);
    }

    public String getSummaryText()
    {
        return txtSummary.getText();
    }

    public void setDetailText(String text)
    {
        txtDetail.setText(text);
    }

    public String getDetailText()
    {
        return txtDetail.getText();
    }

    public void setHeaderIcon(Icon icon)
    {
        lblHeader.setIcon(icon);
    }

    public Icon getHeaderIcon()
    {
        return lblHeader.getIcon();
    }

    public String getThrowableDetail(Throwable t)
    {
        StringWriter sw = new StringWriter(500);
        PrintWriter writer = new PrintWriter(sw);
        StringBuilder sb = new StringBuilder(2500);
        t.printStackTrace(writer);
        sb.append(sw.getBuffer().toString());
        writer.close();
        return sb.toString();
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents)
    {

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(0, 0));

        detailPanel = new JPanel();
        detailPanel.setPreferredSize(new Dimension(600, 250));
        getContentPane().add(detailPanel, BorderLayout.CENTER);
        detailPanel.setLayout(new BorderLayout(0, 0));

        detailPane = new javax.swing.JScrollPane();
        detailPanel.add(detailPane, BorderLayout.CENTER);
        detailPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Additional Detail"));
        txtDetail = new javax.swing.JTextPane();
        detailPane.setViewportView(txtDetail);

        txtDetail.setEditable(false);
        txtDetail.setOpaque(false);

        infoPanel = new JPanel();
        infoPanel.setPreferredSize(new Dimension(600, 250));
        getContentPane().add(infoPanel, BorderLayout.NORTH);
        infoPanel.setLayout(new BorderLayout(0, 0));

        panel = new JPanel();
        infoPanel.add(panel, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(0, 400));
        lblHeader = new javax.swing.JLabel();

        lblHeader.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        summaryPane = new javax.swing.JScrollPane();
        txtSummary = new javax.swing.JTextPane();

        summaryPane.setBorder(null);
        //txtSummary.setContentType("text/html");
        txtSummary.setBorder(null);
        txtSummary.setEditable(false);
        txtSummary.setFocusable(false);
        txtSummary.setOpaque(false);
         
       
        
        summaryPane.setViewportView(txtSummary);
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel.createSequentialGroup().addContainerGap().addComponent(lblHeader).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(summaryPane, GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE).addContainerGap()));
        gl_panel.setVerticalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel.createSequentialGroup().addContainerGap()
                        .addGroup(gl_panel.createParallelGroup(Alignment.TRAILING).addComponent(summaryPane, Alignment.LEADING).addComponent(lblHeader, Alignment.LEADING))
                        .addContainerGap(12, Short.MAX_VALUE)));
        panel.setLayout(gl_panel);

        panel_1 = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel_1.getLayout();
        flowLayout.setAlignment(FlowLayout.TRAILING);
        infoPanel.add(panel_1, BorderLayout.SOUTH);
        btnOK = new javax.swing.JButton();
        panel_1.add(btnOK);

        btnOK.setText("OK");
        btnOK.setPreferredSize(new java.awt.Dimension(100, 23));
        btnOK.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnOKActionPerformed(evt);
            }
        });
        btnOK.setIcon(ImageProvider.instance().getIcon("accept.png"));
        btnCopy = new javax.swing.JButton();
        panel_1.add(btnCopy);

        btnCopy.setText("Copy");
        btnCopy.setPreferredSize(new java.awt.Dimension(100, 23));
        btnCopy.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnCopyActionPerformed(evt);
            }
        });
        btnCopy.setIcon(ImageProvider.instance().getIcon("page_copy.png"));
        btnDetail = new javax.swing.JButton();
        panel_1.add(btnDetail);

        btnDetail.setText("Less >>");
        btnDetail.setPreferredSize(new java.awt.Dimension(100, 23));
        btnDetail.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnDetailActionPerformed(evt);
            }
        });

        pack();

    }// </editor-fold>//GEN-END:initComponents

    private void btnDetailActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnDetailActionPerformed
    {//GEN-HEADEREND:event_btnDetailActionPerformed
        boolean expand = btnDetail.getText().equals("More >>");
        detailPanel.setVisible(expand);
        btnDetail.setText(expand ? "Less <<" : "More >>");
        pack();

    }//GEN-LAST:event_btnDetailActionPerformed

    private void btnCopyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnCopyActionPerformed
    {//GEN-HEADEREND:event_btnCopyActionPerformed
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(txtDetail.getText());
        clipboard.setContents(stringSelection, this);

    }//GEN-LAST:event_btnCopyActionPerformed

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnOKActionPerformed
    {//GEN-HEADEREND:event_btnOKActionPerformed
        dispose();
    }//GEN-LAST:event_btnOKActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCopy;
    private javax.swing.JButton btnDetail;
    private javax.swing.JButton btnOK;
    private javax.swing.JScrollPane summaryPane;
    private javax.swing.JLabel lblHeader;
    private javax.swing.JScrollPane detailPane;
    private javax.swing.JTextPane txtDetail;
    private javax.swing.JTextPane txtSummary;
    private JPanel panel;
    private JPanel detailPanel;
    private JPanel infoPanel;
    private JPanel panel_1;
    // End of variables declaration//GEN-END:variables

}
