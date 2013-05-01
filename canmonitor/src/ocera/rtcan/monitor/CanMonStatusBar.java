package ocera.rtcan.monitor;

import javax.swing.*;
import java.awt.*;

public class CanMonStatusBar {
    public JLabel lbl1;
    public JLabel lbl2;
    public JLabel lbl3;
    public JPanel mainPanel;

    public CanMonStatusBar() {
        mainPanel = new JPanel();

        GridBagLayout gbl = new GridBagLayout();
        mainPanel.setLayout(gbl);
        mainPanel.setEnabled(false);

        final JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        lbl1 = new JLabel();
        lbl1.setIconTextGap(0);
        lbl1.setText("ahoj");
        lbl1.setVerticalAlignment(1);
        lbl1.setVerticalTextPosition(1);
        panel1.add(lbl1);


        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill   = GridBagConstraints.BOTH;
        c.gridheight = 1;
        c.gridwidth  = 3;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 1;
        c.insets = new Insets(0, 8, 8, 0);
        c.ipadx = 0;
        c.ipady = 0;
        c.weightx = 0.9;
        c.weighty = 0.0;
        gbl.setConstraints(panel1, c);
        mainPanel.add(panel1);

        final JPanel panel2 = new JPanel();
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        lbl2 = new JLabel();
        lbl2.setIconTextGap(0);
        lbl2.setText("lbl2");
        lbl2.setVerticalAlignment(1);
        lbl2.setVerticalTextPosition(1);
        panel2.add(lbl2);
        c.weightx = 0.05;
        c.insets = new Insets(0, 4, 8, 0);
        gbl.setConstraints(panel2, c);
        mainPanel.add(panel2);

        final JPanel panel3 = new JPanel();
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        lbl3 = new JLabel();
        lbl3.setHorizontalTextPosition(2);
        lbl3.setIconTextGap(0);
        lbl3.setText("lbl3");
        lbl3.setVerticalAlignment(1);
        lbl3.setVerticalTextPosition(1);
        panel3.add(lbl3);
        c.weightx = 0.05;
        c.insets = new Insets(0, 4, 8, 8);
        gbl.setConstraints(panel3, c);

        mainPanel.add(panel3);
    }
}