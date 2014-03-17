package com.illumina.basespace.igv.ui.tree;

import javax.swing.JFrame;

public class DummyFrame extends JFrame
{
    public DummyFrame(String title)
    {
        super(title);
        setUndecorated(true);
        setVisible(true);
        setLocationRelativeTo(null);
        super.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
       // setIconImages(iconImages);
    }
}
