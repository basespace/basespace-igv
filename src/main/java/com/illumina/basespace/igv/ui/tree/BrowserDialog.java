package com.illumina.basespace.igv.ui.tree;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.illumina.basespace.igv.ui.BusyGlassPaneRenderer;
import com.illumina.basespace.igv.ui.ImageProvider;
import com.illumina.basespace.igv.ui.ProgressReport;

public class BrowserDialog extends JDialog
{
    private static final BrowserDialog INSTANCE = new BrowserDialog();
    private final JPanel contentPanel = new JPanel();
    private BrowserPanel browserPanel;
    private BusyGlassPaneRenderer busy;
    private JButton okButton;
    
    private BrowserDialog()
    {
        super(new DummyFrame("BaseSpace Browser"));
        browserPanel = new BrowserPanel(); 
        addWindowListener(new WindowAdapter() 
        {
            @Override
            public void windowClosing(WindowEvent e) 
            {
                getOwner().dispose();
            }
        });
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        initialize();
        contentPanel.removeAll();
        contentPanel.add(browserPanel);
        contentPanel.invalidate();
    }
    
    public static BrowserDialog instance()
    {
        return INSTANCE;
    }
    
    public BrowserPanel getBrowserPanel()
    {
        return browserPanel;
    }
    
    public void workInit(double max)
    {
        busy = new BusyGlassPaneRenderer(contentPanel, max);
    }
    public void workStart()
    {
        okButton.setEnabled(false);
        busy.showHint();
    }
    public void setWorkMax(int max)
    {
        busy.setProgressMax(max);
    }
    public void workProgress(List<ProgressReport> chunks)
    {
        busy.process(chunks);
    }
    public void workDone()
    {
        busy.dispose();
        okButton.setEnabled(true);
    }
    
    private void initialize()
    {
        setTitle("BaseSpace Browser");
        setBounds(100, 100, 450, 300);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BorderLayout(0, 0));
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                okButton = new JButton("Close");
                okButton.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        setVisible(false);
                        //getOwner().dispose();
                        //dispose();
                    }
                });
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
        }
        this.setIconImage(ImageProvider.instance().getImage("igv-icon-16px.png"));
    }


}
