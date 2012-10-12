/*
 * Created by JFormDesigner on Mon Apr 02 22:36:24 CDT 2012
 */

package org.broad.igv.ui.legend;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

import org.broad.igv.ui.color.*;

/**
 * @author Jim Robinson
 */
public class ColorMapEditor extends JDialog {

    boolean canceled = false;

    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    private JScrollPane scrollPane1;
    private JPanel editPanel;

    private Map<String, Color> changedColors;

    public ColorMapEditor(Frame owner, Map<String, Color> colors) {
        super(owner);
        setModal(true);
        changedColors = new HashMap<String, Color>();
        initComponents();
        initContent(colors);
        setSize(300, 500);
    }

    public Map<String, Color> getChangedColors() {
        if(canceled) {
            changedColors.clear();
        }
        return changedColors;
    }

    private void initContent(final Map<String, Color> colors) {

        for (Map.Entry<String, Color> entry : colors.entrySet()) {
            final JLabel label = new JLabel(entry.getKey());
            final ColorChooserPanel colorChooserPanel = new ColorChooserPanel(entry.getValue());
            editPanel.add(label);
            editPanel.add(colorChooserPanel);

            colorChooserPanel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    String key = label.getText();
                    Color c = colorChooserPanel.getSelectedColor();
                    if(!colors.get(key).equals(c)) {
                         changedColors.put(key, c);
                    }
                }
            });
        }
        editPanel.invalidate();
    }



    public ColorMapEditor(Dialog owner) {
        super(owner);
        initComponents();
    }

    private void initComponents() {
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        scrollPane1 = new JScrollPane();
        editPanel = new JPanel();

        //======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[]{0, 85, 80};
                ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[]{1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        setVisible(false);
                        dispose();
                    }
                });
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        canceled = false;
                        setVisible(false);
                        dispose();
                    }
                });
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
            }
            contentPane.add(buttonBar, BorderLayout.SOUTH);
        }

        //======== scrollPane1 ========
        {
            scrollPane1.setMinimumSize(new Dimension(100, 400));

            //======== contentPanel ========
            {
                editPanel.setMinimumSize(new Dimension(100, 300));
                editPanel.setPreferredSize(new Dimension(200, 400));
                editPanel.setLayout(new GridLayout(0, 2));
            }
            scrollPane1.setViewportView(editPanel);
        }
        contentPane.add(scrollPane1, BorderLayout.CENTER);
        setLocationRelativeTo(getOwner());
    }


}
