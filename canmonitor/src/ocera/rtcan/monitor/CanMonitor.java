/**
 * Created by IntelliJ IDEA.
 * User: Vacek
 * Date: Nov 1, 2002
 * Time: 9:30:53 AM
 * To change this template use Options | File Templates.
 */
package ocera.rtcan.monitor;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import ocera.util.xmlconfig.XmlConfig;
import ocera.util.*;
import ocera.msg.*;
import ocera.rtcan.CanMonClient;
import ocera.rtcan.msg.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import org.jx.xmlgui.XMLMenuBuilder;
import org.jx.xmlgui.XMLActionMapBuilder;
import org.jx.xmlgui.XMLAction;
import org.jx.xmlgui.XMLToolbarBuilder;
import org.flib.FLog;
import org.flib.FString;

class LogTextAreaDocumentFilter extends DocumentFilter
{
    private int maxChars;
    private int charGap;

    LogTextAreaDocumentFilter() {
        this(1000);
    }
    LogTextAreaDocumentFilter(int max_chars) {
        maxChars = max_chars;
        charGap = maxChars / 3;
    }

    public void insertString(FilterBypass fb, int offs, String str, AttributeSet a) throws BadLocationException
    {
        Document doc = fb.getDocument();
        if(doc.getLength() + str.length() > maxChars) {
            doc.remove(0, charGap);
        }
        super.insertString(fb, doc.getLength(), str, a);
    }
}

public class CanMonitor extends JFrame implements Runnable {
    protected JTextField edCanID;
    protected JTextField edCanData0;
    protected JTextField edCanData1;
    protected JTextField edCanData2;
    protected JTextField edCanData3;
    protected JTextField edCanData4;
    protected JTextField edCanData5;
    protected JTextField edCanData6;
    protected JTextField edCanData7;
    protected JTextField[] edCanData = new JTextField[8];

    protected CanMonClient canConn = new CanMonClient();
    //protected int nodeNo = 1;  // number of the node to communicate with

    protected Container pane;
    protected ActionMap actions = new ActionMap();

    protected JPanel panSDOTable;

    protected JTabbedPane tabPane;
    protected JTextArea txtLog;

    protected XmlConfig xmlConfig = new XmlConfig();
    public static final String CONFIG_FILE_NAME = "CanMonitor.conf.xml";
    protected boolean configChanged = false;
    private ConfigLookup confLookup = new ConfigLookup(".canmonitor", CONFIG_FILE_NAME, this.getClass());
    private CanMonStatusBar statusBar = new CanMonStatusBar();
    private JButton btSend;
    //private LinkedList candeviceList = new LinkedList();
    private JButton btClearLog;

    protected JCheckBox cbxShowRoughMessages;
    boolean cbxShowRoughMessages_prev_state = false;

    private JTextField edRawMessagesId;
    private JTextField edRawMessagsMask;
    private JTextField edRawMessagesMask;
    private JPanel lblRawId;
    private JCheckBox cbxCanFlagRTR;
    private JCheckBox cbxCanFlagEXT;
    private JCheckBox cbxCanFlagOVR;
    private JCheckBox cbxCanFlagLOCAL;

    public static void main(String[] args) {
        FLog.logTreshold = FLog.LOG_ERR;
        //FLog.logTreshold = FLog.LOG_DEB;
        CanMonitor app = new CanMonitor(args);
        app.setDefaultCloseOperation(EXIT_ON_CLOSE);
        app.setSize(800, 600);
        app.setVisible(true);
//        app.pack();
    }

    private void openConfigDialog() {
        ConfigDialog xconf = new ConfigDialog(xmlConfig);
        xconf.show();
        configChanged = true;
    }

    private void processCmdLine(String[] args) {
        if (args == null) return;
        String edsFile = null;
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (FString.charAt(s, 0) == '-') {
                char c = FString.charAt(s, 1);
                if (c == 'a') {
                    // address
                    i++;
                    if (i < args.length) {
                        xmlConfig.setValue("/connection/host", args[i]);
                        //canProxyIP = args[i];
                    }
                } else if (c == 'n') {
                    // node
                    i++;
                    if (i < args.length) {
                        xmlConfig.setValue("/canopen/node", args[i]);
                        //xmlConfig.getRootElement().cd("/canopen/node").setValue(args[i]);
                        //nodeNo = FString.toInt(args[i]);
                    }
                } else if (c == 'e') {
                    // EDS file name
                    i++;
                    if (i < args.length) edsFile = args[i];
                } else if (c == 'v') {
                    // verbose
                    FLog.logTreshold = FLog.LOG_INF;
                } else if (c == 'g') {
                    // debug level
                    i++;
                    if (i < args.length) FLog.logTreshold = Integer.parseInt(args[i]);
                    System.err.println("logTreshold: " + FLog.logTreshold);
                } else if (c == 'h') {
                    // help
                    System.out.println("USAGE: cammonitor -a host -n node -e EDS_file_name -v -g debug_level\n");
                    System.exit(0);
                }
            }
        }
        if (edsFile != null) {
            openEDS(edsFile);
        }
    }

    protected void connect() {
        String ip = xmlConfig.getRootElement().cd("/connection/host").getValue("localhost").trim();
        String port = xmlConfig.getValue("/connection/port", "1001").trim();
        int p = FString.toInt(port);
        if (ip.length() > 0) {
            txtLog.append("\nconnecting to " + ip + " ...\n");
            canConn.connect(ip, p);
            txtLog.append(canConn.getErrMsg() + "\n");
        }
        refreshForm();
    }

    protected void disConnect() {
        canConn.disconnect();
        refreshForm();
    }

    public CanMonitor(String[] cmd_line_args) {
        super("CANopen monitor - Beta 0.9");
        boolean asserts_enabled = false;
        assert asserts_enabled = true;


        edCanData[0] = edCanData0;
        edCanData[1] = edCanData1;
        edCanData[2] = edCanData2;
        edCanData[3] = edCanData3;
        edCanData[4] = edCanData4;
        edCanData[5] = edCanData5;
        edCanData[6] = edCanData6;
        edCanData[7] = edCanData7;

        loadConfig();
        processCmdLine(cmd_line_args);
        System.err.println("asserts enabled: " + asserts_enabled);
        System.err.println("Logging level: " + FLog.logTreshold);
        initActions();
        initGui();
        init();
        connect();
//        setBounds(50, 50, 800, 600);
    }

    /**
     * Opens configuration file, triing next locations:<br>
     */
    protected void loadConfig() {
        String conf_file = confLookup.findConfigFile();
        System.out.println("loading config from '" + conf_file + "'");
        if (conf_file != null) xmlConfig.fromURI(conf_file);
        else FLog.log("CanMonitor", FLog.LOG_ERR, "Configuration file " + CONFIG_FILE_NAME + " not found.");
    }

    protected void saveConfig() {
        if (!configChanged) return;
        String conf_file = confLookup.createConfigFile();
        if (conf_file == null) {
            FLog.log("CanMonitor", FLog.LOG_ERR, "Configuration file " + CONFIG_FILE_NAME + " cann't be written.");
            return;
        }

        String s = xmlConfig.toString();
        FFile ff = new FFile(conf_file);
        try {
            ff.writeString(s, FFile.MODE_OVERWRITE, FFile.OVERWRITE_FILE);
        } catch (IOException e) {
            ErrorMsg.show("Error writing config file: " + e);
        } catch (FFileException e) {
            ErrorMsg.show("Error writing config file: " + e);
        }
    }

    public void initActions() {
        //final CanMonitor app = this;

        XMLActionMapBuilder ab = new XMLActionMapBuilder(this);
        actions = ab.buildFrom("resources/menu.xml");

        XMLAction act;
        act = (XMLAction) actions.get("EdsOpen");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                FFileFilter filter = new FFileFilter();
                filter.addExtension("EDS");
                filter.addExtension("eds");
                filter.setDescription("EDS Electronic Data Sheets");
                fc.setFileFilter(filter);
                String pwd = System.getProperty("user.dir", ".");
                fc.setCurrentDirectory(new File(pwd));

                int ret = fc.showOpenDialog(CanMonitor.this);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String fname = fc.getSelectedFile().getAbsolutePath();
                    openEDS(fname);
                }
            }
        };

        act = (XMLAction) actions.get("EdsClose");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int ix = tabPane.getSelectedIndex();
                if (ix > 0) {
                    CANopenDevicePanel candev = (CANopenDevicePanel) tabPane.getComponentAt(ix);
                    tabPane.remove(candev);
                    //candeviceList.remove(candev);
                }
            }
        };

        act = (XMLAction) actions.get("Connect");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        };

        act = (XMLAction) actions.get("Disconnect");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disConnect();
            }
        };

        act = (XMLAction) actions.get("Config");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openConfigDialog();
            }
        };

        act = (XMLAction) actions.get("DevicesReset");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edCanID.setText("0");
                edCanData[0].setText("1");
                edCanData[1].setText("0");
                btSend.getActionListeners()[0].actionPerformed(null);
            }
        };

        act = (XMLAction) actions.get("Quit");
        if (act != null) act.actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cleanUp();
                System.gc();
                System.exit(0);
            }
        };
    }

    private void openEDS(String fname) {
        int ix = tabPane.getTabCount();
        // find free node number
        int node = 0;
        for (int i = 1; i < ix; i++) {
            CANopenDevicePanel p = (CANopenDevicePanel) tabPane.getComponentAt(i);
            int nd = p.getNodeID();
            if (nd > node) node = nd;
        }
        CANopenDevicePanel candev = new CANopenDevicePanel(CanMonitor.this, ix);
        tabPane.add(candev);
        tabPane.setSelectedIndex(ix);    // select tab EDS
        candev.openEDS(fname);
        candev.setNodeID(++node);
    }

    public void initGui() {
        pane = getContentPane();

        //==========================================================
        // menu bar
        //==========================================================
        XMLMenuBuilder builder = new XMLMenuBuilder(this, actions);
        setJMenuBar(builder.buildFrom("resources/menu.xml"));

        //==========================================================
        // tool bar
        //==========================================================
        XMLToolbarBuilder tbb = new XMLToolbarBuilder(this, actions);
        JToolBar tb = tbb.buildFrom("resources/menu.xml");
        pane.add(tb, BorderLayout.NORTH);

        //==========================================================
        //        layout
        //==========================================================
        tabPane.setSelectedIndex(0);    // select tab CAN
        pane.add(tabPane, BorderLayout.CENTER);

        pane.add(statusBar.panel, BorderLayout.SOUTH);
        enableControls();

        //==========================================================
        //        btSend listenner
        //==========================================================
        btSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CANDtgMsg msg = new CANDtgMsg();
                msg.id = FString.toInt(edCanID.getText());
                for (msg.length = 0; msg.length < CANDtgMsg.DATA_LEN_MAX; ) {
                    String s = edCanData[msg.length].getText().trim();
                    if (s.length() == 0) break;
                    msg.data[msg.length++] = (byte) FString.toInt(s, 10);
                }
                if (cbxCanFlagRTR.isSelected()) msg.flags |= CANDtgMsg.MSG_RTR;
                if (cbxCanFlagEXT.isSelected()) msg.flags |= CANDtgMsg.MSG_EXT;
                if (cbxCanFlagOVR.isSelected()) msg.flags |= CANDtgMsg.MSG_OVR;
                if (cbxCanFlagLOCAL.isSelected()) msg.flags |= CANDtgMsg.MSG_LOCAL;
                txtLog.append("SENDING:\t" + msg);
                canConn.send(msg);
            }
        });

        btClearLog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                txtLog.setText("");
            }
        });

        //==========================================================
        //        cbxShowRoughMessages listenner
        //==========================================================
        cbxShowRoughMessages.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JCheckBox cb = (JCheckBox) e.getSource();
                ServiceSetRawMsgParamsRequest rq = new ServiceSetRawMsgParamsRequest();
                boolean state = cb.isSelected();
                // one click on checkbox generate 5 listenner calls
                if (state != cbxShowRoughMessages_prev_state) {
                    cbxShowRoughMessages_prev_state = state;
                    if (state == true) {
                        rq.command = ServiceSetRawMsgParamsRequest.ADD;
                        rq.id = FString.toInt(edRawMessagesId.getText(), 16);
                        rq.mask = FString.toInt(edRawMessagesMask.getText(), 16);
                        cb.setBackground(Color.ORANGE);
                    } else {
                        rq.command = ServiceSetRawMsgParamsRequest.REMOVE_ALL;
                    }
                    //txtLog.append("event " + cb.isSelected() + "\n");
                    txtLog.append("SENDING:\t" + rq);
                    canConn.send(rq);
                }
            }
        });

        //==========================================================
        //        JTextArea logs listenners
        //==========================================================
        int logsize = FString.toInt(xmlConfig.getValue("/logging/logscreensize", "1000000"));
        ((AbstractDocument) txtLog.getDocument()).setDocumentFilter(new LogTextAreaDocumentFilter(logsize));

        //==========================================================
        //        JFrame listenners
        //==========================================================
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actions.get("Quit").actionPerformed(null);
            }
        });
    }

    private void refreshForm() {
        boolean connected = (canConn.getSocket() != null);
        XMLAction act;
        act = (XMLAction) actions.get("Connect");
        act.setEnabled(!connected);
        act = (XMLAction) actions.get("Disconnect");
        act.setEnabled(connected);
        if (!connected) {
            statusBar.lbl1.setText("disconnected");
        } else {
            statusBar.lbl1.setText(canConn.getSocket().toString());
        }
        statusBar.lbl2.setText("");
        statusBar.lbl3.setText("");
    }

    public void setTabLabel(int tabix, String lbl) {
        tabPane.setTitleAt(tabix, lbl);
    }

    private void enableControls() {
    }

    public void init() {
        canConn.setGuiUpdate(this);
//        canConn.connect(serverAddr, 0);

        Thread t = new Thread(this);
        t.start();
    }

    public void cleanUp() {
//        if(canReaderThread != null)
//            try {canReaderThread.join();} catch(InterruptedException e) {}
        disConnect();
        saveConfig();
    }

    /**
     * sends msg to the socket if it is opened
     */
    void sendMessage(Object o) {
        if (canConn == null) return;
        if (!canConn.connected()) return;
        canConn.send(o);
    }

    private int msgCount = 0;

    /**
     * code refreshing GUI controls from variables set by other thread
     * mainly CanMonClient thread
     */
    public void run() {
        // read all received messages if any
        while (true) {
            Object o = canConn.readQueue.remove(RoundQueue.NO_BLOCKING);
            if (o == null) break;
            FLog.log("CanMonitor", FLog.LOG_DEB, "received object " + o);

            if (o instanceof CANDtgMsg) {
                msgCount++;
                if (cbxShowRoughMessages.isSelected()) txtLog.append("RECEIVE[" + msgCount + "]:\t" + o);
            } else if (o instanceof ServiceSetRawMsgParamsConfirm) {
                cbxShowRoughMessages.setBackground(lblRawId.getBackground());
                ServiceSetRawMsgParamsConfirm spc = (ServiceSetRawMsgParamsConfirm) o;
                if (spc.code != ServiceSetRawMsgParamsConfirm.OK) {
                    ErrorMsg.show(tabPane, spc.errmsg);
                }
            } else {
                // scan all CANopen devices
                for (int i = 1; i < tabPane.getTabCount(); i++) {
                    CANopenDevicePanel p = (CANopenDevicePanel) tabPane.getComponentAt(i);
                    if (p.tasteObject(o)) break;
                }
            }
        }
        // refresh form
        refreshForm();
        //FLog.log(getClass().getName(), FLog.LOG_TRASH, "run(): GUI updated");
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        tabPane = new JTabbedPane();
        tabPane.setTabLayoutPolicy(0);
        tabPane.setTabPlacement(1);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(3, 3, 0, 0), 3, -1));
        tabPane.addTab("CAN", panel1);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        txtLog = new JTextArea();
        scrollPane1.setViewportView(txtLog);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 11, new Insets(0, 0, 0, 0), 5, 0));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edCanID = new JTextField();
        panel3.add(edCanID, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(50, -1), null, null, 0, false));
        edCanData0 = new JTextField();
        panel3.add(edCanData0, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edCanData4 = new JTextField();
        panel3.add(edCanData4, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edCanData6 = new JTextField();
        panel3.add(edCanData6, new GridConstraints(1, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setIconTextGap(0);
        label1.setText("ID");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setIconTextGap(0);
        label2.setText("byte[0]");
        panel3.add(label2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setIconTextGap(0);
        label3.setText("byte[4]");
        panel3.add(label3, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setIconTextGap(0);
        label4.setText("byte[6]");
        panel3.add(label4, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setIconTextGap(0);
        label5.setText("byte[2]");
        panel3.add(label5, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edCanData2 = new JTextField();
        panel3.add(edCanData2, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setIconTextGap(0);
        label6.setText("byte[1]");
        panel3.add(label6, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setIconTextGap(0);
        label7.setText("byte[3]");
        panel3.add(label7, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setIconTextGap(0);
        label8.setText("byte[5]");
        panel3.add(label8, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setIconTextGap(0);
        label9.setText("byte[7]");
        panel3.add(label9, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edCanData1 = new JTextField();
        panel3.add(edCanData1, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edCanData3 = new JTextField();
        panel3.add(edCanData3, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edCanData5 = new JTextField();
        panel3.add(edCanData5, new GridConstraints(1, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edCanData7 = new JTextField();
        panel3.add(edCanData7, new GridConstraints(1, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cbxCanFlagRTR = new JCheckBox();
        cbxCanFlagRTR.setText("RTR");
        panel3.add(cbxCanFlagRTR, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cbxCanFlagEXT = new JCheckBox();
        cbxCanFlagEXT.setText("EXT");
        panel3.add(cbxCanFlagEXT, new GridConstraints(1, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cbxCanFlagOVR = new JCheckBox();
        cbxCanFlagOVR.setText("OVR");
        panel3.add(cbxCanFlagOVR, new GridConstraints(0, 10, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cbxCanFlagLOCAL = new JCheckBox();
        cbxCanFlagLOCAL.setText("LOCAL");
        panel3.add(cbxCanFlagLOCAL, new GridConstraints(1, 10, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btSend = new JButton();
        btSend.setText("Send");
        btSend.setMnemonic('S');
        btSend.setDisplayedMnemonicIndex(0);
        panel2.add(btSend, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        lblRawId = new JPanel();
        lblRawId.setLayout(new GridLayoutManager(1, 11, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(lblRawId, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cbxShowRoughMessages = new JCheckBox();
        cbxShowRoughMessages.setAutoscrolls(false);
        cbxShowRoughMessages.setBorderPainted(false);
        cbxShowRoughMessages.setBorderPaintedFlat(false);
        cbxShowRoughMessages.setContentAreaFilled(true);
        cbxShowRoughMessages.setEnabled(true);
        cbxShowRoughMessages.setSelected(false);
        cbxShowRoughMessages.setText("Show rough messages");
        cbxShowRoughMessages.setMnemonic('S');
        cbxShowRoughMessages.setDisplayedMnemonicIndex(0);
        lblRawId.add(cbxShowRoughMessages, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btClearLog = new JButton();
        btClearLog.setText("Clear log");
        btClearLog.setVerticalAlignment(0);
        lblRawId.add(btClearLog, new GridConstraints(0, 10, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        lblRawId.add(spacer2, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        edRawMessagesId = new JTextField();
        lblRawId.add(edRawMessagesId, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(70, -1), null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("ID");
        lblRawId.add(label10, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        lblRawId.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, 1, new Dimension(30, -1), null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("Mask");
        lblRawId.add(label11, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        edRawMessagesMask = new JTextField();
        lblRawId.add(edRawMessagesMask, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(70, -1), null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("hex");
        lblRawId.add(label12, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("hex");
        lblRawId.add(label13, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        lblRawId.add(spacer4, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, 1, new Dimension(30, -1), null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return tabPane;
    }
}
